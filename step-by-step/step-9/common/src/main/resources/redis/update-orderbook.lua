--[[
刷新OrderBook快照:

KEYS:
  1: key: OrderBook快照的key

ARGV:
  1: seqId: 本次的SequenceId
  2: data: OrderBook快照的JSON数据

]]--

local KEY_LAST_SEQ = '_OBLastSeqId_'
local key = KEYS[1]
local seqId = ARGV[1]
local data = ARGV[2]

-- 获取上次更新的sequenceId:
local lastSeqId = redis.call('GET', KEY_LAST_SEQ)

-- 如果sequenceId较新:
if not lastSeqId or tonumber(seqId) > tonumber(lastSeqId) then
    -- 保存新的sequenceId:
    redis.call('SET', KEY_LAST_SEQ, seqId)
    -- 保存OrderBook的JSON:
    redis.call('SET', key, data)
    -- 发送通知:
    redis.call('PUBLISH', 'notification', '{"type":"orderbook","data":' .. data .. '}')
    return true
end

return false
