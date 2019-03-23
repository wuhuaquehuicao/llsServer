#! /usr/bin/env python
# coding=utf-8
import sys
import signal
import commands
import os
import socket
from contextlib import closing
from optparse import OptionParser


s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
s.connect(("8.8.8.8", 80))
PUBLIC_IP = s.getsockname()[0]
s.close()

MASTER_IP = PUBLIC_IP
IMAGE_NAME = "redis:3.2.11-alpine"

parser = OptionParser()
parser.add_option(
    "--master_ip",
    action="store",
    dest="master_ip",
    type="string",
    default=PUBLIC_IP,
    help="master ip, default " + PUBLIC_IP
)

parser.add_option(
    "--ip",
    action="store",
    dest="ip",
    type="string",
    default=PUBLIC_IP,
    help="public ip, default " + PUBLIC_IP
)

parser.add_option(
    "--image",
    action="store",
    dest="image",
    type="string",
    default=IMAGE_NAME,
    help="docker image name, default " + IMAGE_NAME
)

parser.add_option(
    "--master",
    action="store_true",
    dest="master",
    default=False,
    help="create master"
)

parser.add_option(
    "--slave",
    action="store",
    dest="slave_index",
    type="int",
    help="create slave with index"
)

parser.add_option(
    "--sentinel",
    action="store",
    dest="sentinel_index",
    type="int",
    help="create sentinel with index"
)

(options, args) = parser.parse_args()

MASTER_IP = options.master_ip
PUBLIC_IP = options.ip
IMAGE_NAME = options.image

# print options
# print args

if __name__ == '__main__':
    try:
        reload(sys)
        sys.setdefaultencoding('utf-8')

        def close_handler(sig, frame):
            print sig
            print frame
            sys.exit()
        if len(sys.argv) < 2:
            parser.print_help()
            sys.exit()

        if len(args) > 0:
            if args[0] == "stop":
                ds = commands.getoutput("docker ps --format \"{{.Names}}\"")
                ds2 = ds.split("\n")
                for line in ds2:
                    if line.startswith("redis_"):
                        commands.getoutput("docker stop " + line)
            elif args[0] == "rm":
                ds = commands.getoutput("docker ps -a --format \"{{.Names}}\"")
                ds2 = ds.split("\n")
                for line in ds2:
                    if line.startswith("redis_"):
                        commands.getoutput("docker rm " + line)
            elif args[0] == "ls":
                ds = commands.getoutput("docker ps -a --format \"{{.Names}}\t{{.Ports}}\t{{.ID}}\"")
                ds2 = ds.split("\n")
                for line in ds2:
                    if line.startswith("redis_"):
                        print line
            else:
                print "unknown command %s" % args[0]
            sys.exit()
        if options.master:
            SERVICES_NODE = "master"
            INDEX = 1
        elif options.slave_index:
            INDEX = options.slave_index
            SERVICES_NODE = "slave"
        elif options.sentinel_index:
            INDEX = options.sentinel_index
            SERVICES_NODE = "sentinel"
        else:
            print "unknown node"
            sys.exit()

        signal.signal(signal.SIGINT, close_handler)

        SERVICES_NAME = "redis"
        MASTER_PORT = 6379

        CONTAINER_HOME = os.path.expanduser("~/Containers")
        # WORKSPACE = os.path.split(os.path.realpath(__file__))[0]
        # CONTAINER_HOME = WORKSPACE + "/Containers"

        if not os.path.exists(CONTAINER_HOME):
            os.mkdir(CONTAINER_HOME)

        redis_conf_file = '''bind 0.0.0.0
protected-mode no
tcp-backlog 511
timeout 0
tcp-keepalive 300
daemonize no
supervised no
pidfile /var/run/redis.pid
loglevel notice
logfile ""
databases 16
save 900 1
save 300 10
save 60 10000
stop-writes-on-bgsave-error yes
rdbcompression yes
rdbchecksum yes
dbfilename dump.rdb
dir ./
slave-serve-stale-data yes
slave-read-only yes
repl-diskless-sync no
repl-diskless-sync-delay 5
repl-disable-tcp-nodelay no
slave-priority 100
appendonly no
appendfilename "appendonly.aof"
appendfsync everysec
no-appendfsync-on-rewrite no
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb
aof-load-truncated yes
lua-time-limit 5000
slowlog-log-slower-than 10000
slowlog-max-len 128
latency-monitor-threshold 0
notify-keyspace-events ""
hash-max-ziplist-entries 512
hash-max-ziplist-value 64
list-max-ziplist-size -2
list-compress-depth 0
set-max-intset-entries 512
zset-max-ziplist-entries 128
zset-max-ziplist-value 64
hll-sparse-max-bytes 3000
activerehashing yes
client-output-buffer-limit normal 0 0 0
client-output-buffer-limit slave 256mb 64mb 60
client-output-buffer-limit pubsub 32mb 8mb 60
hz 10
aof-rewrite-incremental-fsync yes
'''

        def find_free_port():
            with closing(socket.socket(socket.AF_INET, socket.SOCK_STREAM)) as s:
                s.bind(('', 0))
                return int(s.getsockname()[1])

        def is_port_available(port):
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            result = sock.connect_ex(('127.0.0.1', port))
            if result == 0:
                return False
            else:
                return True

        cmd = ""
        if SERVICES_NODE == "master":
            if is_port_available(MASTER_PORT):
                PORT = MASTER_PORT
            else:
                print "port %s is unavailable for master" % MASTER_PORT
                sys.exit()
        elif SERVICES_NODE == "slave":
            PORT = MASTER_PORT + INDEX
        elif SERVICES_NODE == "sentinel":
            PORT = 20000 + MASTER_PORT + INDEX
        if not is_port_available(PORT):
            PORT = find_free_port()

        if SERVICES_NODE == "master":
            CONTAINER_NAME = SERVICES_NAME + "_" + SERVICES_NODE
            CONTAINER_PATH = CONTAINER_HOME + "/" + CONTAINER_NAME
            if not os.path.exists(CONTAINER_PATH):
                os.mkdir(CONTAINER_PATH)
            PORT = MASTER_PORT
            f = open(CONTAINER_PATH + "/redis.conf", "w")
            f.write(redis_conf_file)
            f.write("port " + str(PORT) + "\n")
            f.write("slave-announce-ip " + PUBLIC_IP + "\n")
            f.close()
            cmd = "redis-server /data/redis.conf"

        elif SERVICES_NODE == "slave":
            CONTAINER_NAME = SERVICES_NAME + "_" + SERVICES_NODE + "_" + str(INDEX)
            CONTAINER_PATH = CONTAINER_HOME + "/" + CONTAINER_NAME
            if not os.path.exists(CONTAINER_PATH):
                os.mkdir(CONTAINER_PATH)
            f = open(CONTAINER_PATH + "/redis.conf", "w")
            f.write(redis_conf_file)
            f.write("port " + str(PORT) + "\n")
            f.write("slave-announce-ip " + PUBLIC_IP + "\n")
            f.close()
            cmd = "redis-server /data/redis.conf --slaveof %s %d" % (MASTER_IP, MASTER_PORT)

        elif SERVICES_NODE == "sentinel":
            CONTAINER_NAME = SERVICES_NAME + "_" + SERVICES_NODE + "_" + str(INDEX)
            CONTAINER_PATH = CONTAINER_HOME + "/" + CONTAINER_NAME
            if not os.path.exists(CONTAINER_PATH):
                os.mkdir(CONTAINER_PATH)
            f = open(CONTAINER_PATH + "/redis.conf", "w")
            f.write("sentinel announce-ip " + PUBLIC_IP + "\n")
            f.write("port " + str(PORT) + "\n")
            f.write("sentinel monitor mymaster " + MASTER_IP + " " + str(MASTER_PORT) + " 2\n")
            f.write("sentinel down-after-milliseconds mymaster 5000\n")
            f.write("sentinel parallel-syncs mymaster 1\n")
            f.write("sentinel failover-timeout mymaster 5000\n")
            f.close()
            cmd = "redis-server /data/redis.conf --sentinel"

        else:
            print "usage: " + sys.argv[0] + " master/slave/sentinel"
            sys.exit()

        docker_run = "docker run -d --label created=py --net doc_default -it --name %s --sysctl=net.core.somaxconn=262144 --ulimit nofile=10036:10036 -v %s:/data -p %d:%d  %s %s" % (CONTAINER_NAME, CONTAINER_PATH, PORT, PORT, IMAGE_NAME, cmd)
        print commands.getoutput(docker_run)
    except KeyboardInterrupt:
        print 'KeyboardInterrupt'
