
class ObjectB:

    flag = False  # bool

    def protocolId(self):
        return 103

    @classmethod
    def write(cls, buffer, packet):
        if packet is None:
            buffer.writeInt(0)
            return
        buffer.writeInt(-1)
        buffer.writeBool(packet.flag)
        pass

    @classmethod
    def read(cls, buffer):
        length = buffer.readInt()
        if length == 0:
            return None
        beforeReadIndex = buffer.getReadOffset()
        packet = ObjectB()
        result0 = buffer.readBool() 
        packet.flag = result0
        if length > 0:
            buffer.setReadOffset(beforeReadIndex + length)
        return packet

