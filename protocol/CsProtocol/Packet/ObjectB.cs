using System;
using System.Collections.Generic;
using CsProtocol.Buffer;

namespace CsProtocol
{
    
    public class ObjectB : IProtocol
    {
        public bool flag;

        public static ObjectB ValueOf(bool flag)
        {
            var packet = new ObjectB();
            packet.flag = flag;
            return packet;
        }


        public short ProtocolId()
        {
            return 103;
        }
    }


    public class ObjectBRegistration : IProtocolRegistration
    {
        public short ProtocolId()
        {
            return 103;
        }

        public void Write(ByteBuffer buffer, IProtocol packet)
        {
            if (buffer.WritePacketFlag(packet))
            {
                return;
            }
            ObjectB message = (ObjectB) packet;
            buffer.WriteBool(message.flag);
        }

        public IProtocol Read(ByteBuffer buffer)
        {
            if (!buffer.ReadBool())
            {
                return null;
            }
            ObjectB packet = new ObjectB();
            bool result0 = buffer.ReadBool();
            packet.flag = result0;
            return packet;
        }
    }
}
