from uuid import getnode

def get_serial():
    try:
        f = open('/proc/cpuinfo', 'r')
        serial = None
        for line in f:
            if line[0:6] == 'Serial':
                serial = line[10:26]
        f.close()
    except:
        serial = None
    return serial

def get_mac():
    mac = getnode()
    if (mac >> 40) % 2:
        return None
    else:
        return ':'.join(("%012x" % mac)[i:i+2] for i in range(0, 12, 2))

def get():
    serial = get_serial()
    mac = get_mac()
    if serial:
        return "sysid:%s" % serial
    elif mac:
        return "eth:%s" % mac
    else:
        raise OSError, "cannot determine a unique system identifier"

if __name__ == "__main__":
    print "id:", get()
