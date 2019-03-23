package com.lssservlet.managers;

import java.util.ArrayList;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lssservlet.core.Config;
import com.lssservlet.core.DataManager;
import com.lssservlet.datamodel.ADSDbKey;
import com.lssservlet.datamodel.ADSDbKey.Type;
import com.lssservlet.datamodel.ADSToken;
import com.lssservlet.datamodel.ADSToken.TokenValue;
import com.lssservlet.datamodel.ADSUser;
import com.lssservlet.exception.ErrorCode;
import com.lssservlet.utils.AlphaId;
import com.lssservlet.utils.Codec;
import com.lssservlet.utils.DataException;
import com.lssservlet.utils.EmailUtil;
import com.lssservlet.utils.TaskManager;

public class UserManager extends BaseManager {
    private static volatile UserManager sInstance = null;
    private static int HASH_STRING_LENGTH = 20;
    protected static final Logger log = LogManager.getLogger(UserManager.class);

    private UserManager() {
    }

    public static UserManager getInstance() {
        if (sInstance == null) {
            synchronized (UserManager.class) {
                if (sInstance == null) {
                    sInstance = new UserManager();
                }
            }
        }
        return sInstance;
    }

    public ADSUser getUserByName(String name) {
        if (name == null || name.length() == 0)
            return null;
        String id = DataManager.getInstance().getUserNameMap().get(name.toLowerCase());
        if (id != null) {
            return ADSUser.getUser(id);
        }
        return null;
    }

    public ArrayList<ADSUser> getUsers(boolean ascending) {
        return DataManager.getInstance().getSortDataList(Type.EUser, ascending);
    }

    public ADSUser createUser(String name, String adminId) throws DataException {
        ADSUser user = getUserByName(name);
        if (user != null)
            throw new DataException(ErrorCode.USER_DUPLICATED_NAME, "Duplicated name.");
        if (adminId == null)
            throw new DataException(ErrorCode.USER_ROLE_ERROR, "No admin user.");

        ADSUser admin = ADSUser.getUser(adminId);
        if (admin == null || !admin.isAdminUser())
            throw new DataException(ErrorCode.USER_ROLE_ERROR, "Only admin can create user.");

        user = new ADSUser();
        user.id = AlphaId.generateID();
        user.name = name;
        String initPassword = AlphaId.generateID();
        user.salt = generatSalt();
        user.password = Codec.hmacSha256(user.salt, initPassword);
        user.role = ADSDbKey.Role_User;
        user.created_by = adminId;
        user.created_at = DataManager.getInstance().dbtime();
        user.updated_at = user.created_at;
        user.original_pwd = 1;
        user.flag = 0;
        user.update(true);
        sendPwdEmail(name, user.id, initPassword);
        return user;
    }

    public ADSUser updateUser(String userId, String oldPwd, String pwd) throws DataException {
        ADSUser user = ADSUser.getUser(userId);
        if (user == null)
            throw new DataException(ErrorCode.USER_NOT_FOUND, "Not found user.");

        String pString = Codec.hmacSha256(user.salt, oldPwd);
        if (user.password == null || !user.password.equals(pString))
            throw new DataException(ErrorCode.USER_PASSWORD_ERROR, "Invalid password");

        user.password = Codec.hmacSha256(user.salt, pwd);
        user.role = ADSDbKey.Role_User;
        user.updated_at = DataManager.getInstance().dbtime();
        user.original_pwd = 0;
        user.update(true);
        return user;
    }

    public ADSUser resetUserPwd(String userId, String adminId) throws DataException {
        if (adminId == null)
            throw new DataException(ErrorCode.USER_ROLE_ERROR, "No admin user.");

        ADSUser admin = ADSUser.getUser(adminId);
        if (admin == null || !admin.isAdminUser())
            throw new DataException(ErrorCode.USER_ROLE_ERROR, "Only admin can create user.");

        ADSUser user = ADSUser.getUser(userId);
        if (user == null)
            throw new DataException(ErrorCode.USER_NOT_FOUND, "Not found user.");

        String initPassword = AlphaId.generateID();
        user.salt = generatSalt();
        user.password = Codec.hmacSha256(user.salt, initPassword);
        user.original_pwd = 1;
        user.updated_at = DataManager.getInstance().dbtime();
        user.update(true);
        sendPwdEmail(user.name, user.id, initPassword);
        return user;
    }

    public void deleteUser(String userId) throws DataException {
        ADSUser user = ADSUser.getUser(userId);
        if (user != null)
            user.delete(true);
    }

    private static String generatSalt() {
        String hash = "";
        for (int index = 0; index < HASH_STRING_LENGTH; index++) {
            Random random = new Random();
            Integer value = Math.abs(random.nextInt() % 10);
            hash += String.format("%d", value);
        }
        return hash;
    }

    public void sendPwdEmail(String dest, String userId, String vcode) {
        String template = "<html>\r\n" + "<head>\r\n"
                + "<meta content=\"text/html charset=utf-8\" http-equiv=\"Content-Type\">" + "</head>\r\n"
                + "<body class=\"\" style=\"word-wrap: break-word; -webkit-nbsp-mode: space; -webkit-line-break: after-white-space;\">\r\n"
                + "Thanks for joining TOMA Media! <br>" + "Your password is: " + vcode + "<br>"
                // + "Please click this link to change it if you want to do that. <br>" + "<a href=https://"
                // + Config.getInstance().getServerUrl() + "/api/v2/users/" + userId + "/update?vcode=" + vcode
                // + ">/api/v2/users/" + userId + "/activate?vcode=" + vcode + "&time=" +
                // DataManager.getInstance().time()
                // + "</a> <br>" + "If you cannot click it, please copy it and open it in a browser."
                + "</body>\r\n" + "</html>";
        // send email
        if (com.lssservlet.core.Config.getInstance()._serverType != 3) {
            TaskManager.runTaskOnThreadPool("acitvateAccountTask", 5, handler -> {
                // String html = StringUtil.txtToHtml(template);
                try {
                    EmailUtil.getInstance(null).sendMail(dest, null, "Password for the TOMA Media Account", template);
                } catch (Exception e) {
                    log.warn("email log error", e);
                }
            });
        }
    }

    public String createToken(TokenValue value, String password, Long expired_at, int type) {
        String tokenId = Codec.md5(AlphaId.getUniqueId(Config.getInstance().getServerId()).toString());
        // String tokenId = UUID.randomUUID().toString().replace("-", "");
        ADSToken token = new ADSToken();
        token.id = tokenId;
        token.value = value;
        if (expired_at != null && expired_at > 0)
            token.expired_at = DataManager.getInstance().time() + expired_at * 1000;
        token.update(true);
        return tokenId;
    }

    public ADSToken getToken(String id) {
        if (id == null || id.length() == 0)
            return null;
        ADSToken token = DataManager.getInstance().getCache(ADSToken.getCacheKey(id));
        if (token != null && token.flag == 0) {
            if (token.isExpired()) {
                token.delete(true);
                return null;
            }
            return token;
        }
        return null;
    }
}
