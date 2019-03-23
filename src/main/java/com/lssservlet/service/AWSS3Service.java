package com.lssservlet.service;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.lssservlet.core.Config;

public class AWSS3Service {
    protected static final Logger log = LogManager.getLogger(AWSS3Service.class);
    // private static final String ACCESS_KEY_ID = "AKIAINUFXZQS5VMJZXFA";// "AKIAI6KZ22L5ZGUGDDVA";
    // private static final String SECRET_KEY = "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";//
    // "RAMkpjDnGGxYMNSoabKDdxEvs+qZx6ioH8sPsm0Y";

    public static void deleteFile(String bucketName, String bucketKey, String fileName) throws IOException {
        AmazonS3 s3Client = new AmazonS3Client(new BasicAWSCredentials(Config.getInstance().getAwsAccessKey(),
                Config.getInstance().getAwsSecretKey()));
        Region usWest2 = Region.getRegion(Regions.US_WEST_2);// us-east-1
        s3Client.setRegion(usWest2);

        try {
            s3Client.deleteObject(new DeleteObjectRequest(bucketName, bucketKey + "/" + fileName));
        } catch (AmazonServiceException ase) {
            log.error(ase + " bucketName: " + bucketName + ", bucketFolder: " + bucketKey + "/" + fileName);
        } catch (AmazonClientException ace) {
            log.error(ace + " bucketName: " + bucketName + ", bucketFolder: " + bucketKey + "/" + fileName);
        }
    }

    public static void uploadFile(String bucketName, String bucketKey, File file) throws IOException {
        AmazonS3 s3Client = new AmazonS3Client(new BasicAWSCredentials(Config.getInstance().getAwsAccessKey(),
                Config.getInstance().getAwsSecretKey()));
        Region usWest2 = Region.getRegion(Regions.US_WEST_2);// us-east-1
        s3Client.setRegion(usWest2);
        PutObjectRequest request = new PutObjectRequest(bucketName, bucketKey, file);

        try {
            s3Client.putObject(request);
            s3Client.setObjectAcl(bucketName, bucketKey, CannedAccessControlList.PublicRead);
        } catch (AmazonServiceException ase) {
            log.error(ase + " bucketName: " + bucketName + ", bucketKey: " + bucketKey);
        } catch (AmazonClientException ace) {
            log.error(ace + " bucketName: " + bucketName + ", bucketKey: " + bucketKey);
        }
    }

    public static void main(String[] args) throws IOException {
        String bucketName = "goldpass";
        String bucketFolder = "review/Yelp_phUdcIxWnhz_pa6rXYWBpw";
        String fileName = "Rave_jim@justek.us-20180711090016.mp4";

        AmazonS3 s3Client = new AmazonS3Client(new BasicAWSCredentials(Config.getInstance().getAwsAccessKey(),
                Config.getInstance().getAwsSecretKey()));
        Region usWest2 = Region.getRegion(Regions.US_EAST_1);// us-east-1
        s3Client.setRegion(usWest2);

        try {
            s3Client.deleteObject(new DeleteObjectRequest(bucketName, bucketFolder + "/" + fileName));
        } catch (AmazonServiceException ase) {
            log.error(ase);
        } catch (AmazonClientException ace) {
            log.error(ace);
        }
    }
}
