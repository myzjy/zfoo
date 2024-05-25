
class ObjectA:

    a = 0  # int
    m = {}  # Dictionary<int, string>
    objectB = None  # ObjectB

    def protocolId(self):
        return 102

    @classmethod
    def write(cls, buffer, packet):
        if packet is None:
            buffer.writeInt(0)
            return
        buffer.writeInt(-1)
        buffer.writeInt(packet.a)
        buffer.writeIntStringMap(packet.m)
        buffer.writePacket(packet.objectB, 103)
        pass

    @classmethod
    def read(cls, buffer):
        length = buffer.readInt()
        if length == 0:
            return None
        beforeReadIndex = buffer.getReadOffset()
        packet = ObjectA()
        result0 = buffer.readInt()
        packet.a = result0
        map1 = buffer.readIntStringMap()
        packet.m = map1
        result2 = buffer.readPacket(103)
        packet.objectB = result2
        if length > 0:
            buffer.setReadOffset(beforeReadIndex + length)
        return packet

