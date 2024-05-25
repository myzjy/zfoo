
local EmptyObject = {}

function EmptyObject:new()
    local obj = {
        
    }
    setmetatable(obj, self)
    self.__index = self
    return obj
end

function EmptyObject:protocolId()
    return 0
end

function EmptyObject:write(buffer, packet)
    if buffer:writePacketFlag(packet) then
        return
    end
    
end

function EmptyObject:read(buffer)
    if not(buffer:readBoolean()) then
        return nil
    end
    local packet = EmptyObject:new()
    
    return packet
end

return EmptyObject
