--[[

根据sequenceId判断是否需要发送tick通知

KEYS:
  1: 最新Ticks的Key

ARGV:
  1: sequenceId
  2: JSON字符串表示的tick数组："[{...},{...},...]"
  3: JSON字符串表示的tick数组："["{...}","{...}",...]"
--]]

local KEY_LAST_SEQ = '_TickSeq_' -- 上次更新的SequenceID
local LIST_RECENT_TICKS = KEYS[1] -- 最新Ticks的Key

local seqId = ARGV[1] -- 输入的SequenceID
local jsonData = ARGV[2] -- 输入的JSON字符串表示的tick数组："[{...},{...},...]"
local strData = ARGV[3] -- 输入的JSON字符串表示的tick数组："["{...}","{...}",...]"

-- 获取上次更新的sequenceId:
local lastSeqId = redis.call('GET', KEY_LAST_SEQ)
local ticks, len;

if not lastSeqId or tonumber(seqId) > tonumber(lastSeqId) then
    -- 广播:
    redis.call('PUBLISH', 'notification', '{"type":"tick","sequenceId":' .. seqId .. ',"data":' .. jsonData .. '}')
    -- 保存当前sequence id:
    redis.call('SET', KEY_LAST_SEQ, seqId)
    -- 更新最新tick列表:
    ticks = cjson.decode(strData)
    len = redis.call('RPUSH', LIST_RECENT_TICKS, unpack(ticks))
    if len > 100 then
        -- 裁剪LIST以保存最新的100个Tick:
        redis.call('LTRIM', LIST_RECENT_TICKS, len-100, len-1)
    end
    return true
end

-- 无更新返回false
return false
