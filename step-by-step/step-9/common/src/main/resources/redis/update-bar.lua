--[[
  根据sequenceId和已合并的tick更新Bar数据

  参数：
  KEYS:
    1. sec-bar的key
    2. min-bar的key
    3. hour-bar的key
    4. day-bar的key
  ARGV:
    1. sequenceId
    2. secTimestamp
    3. minTimestamp
    4. hourTimestamp
    5. dayTimestamp
    6. openPrice
    7. highPrice
    8. lowPrice
    9. closePrice
    10. quantity

  Redis存储的Bar数据结构：[timestamp, open, high, low, close, quantity, volume]
  ZScoredSet:
    key: '_day_bars_'
    key: '_hour_bars_'
    key: '_min_bars_'
    key: '_sec_bars_'
  Key: _BarSeq_ 存储上次更新的SequenceId
--]]

local function merge(existBar, newBar)
    existBar[3] = math.max(existBar[3], newBar[3]) -- 更新High Price
    existBar[4] = math.min(existBar[4], newBar[4]) -- 更新Low Price
    existBar[5] = newBar[5] -- close
    existBar[6] = existBar[6] + newBar[6] -- 更新quantity
end

local function tryMergeLast(barType, seqId, zsetBars, timestamp, newBar)
    local topic = 'notification'
    local popedScore, popedBar
    -- 查找最后一个Bar:
    local poped = redis.call('ZPOPMAX', zsetBars)
    if #poped == 0 then
        -- ZScoredSet无任何bar, 直接添加:
        redis.call('ZADD', zsetBars, timestamp, cjson.encode(newBar))
        redis.call('PUBLISH', topic, '{"type":"bar","resolution":"' .. barType .. '","sequenceId":' .. seqId .. ',"data":' .. cjson.encode(newBar) .. '}')
    else
        popedBar = cjson.decode(poped[1])
        popedScore = tonumber(poped[2])
        if popedScore == timestamp then
            -- 合并Bar并发送通知:
            merge(popedBar, newBar)
            redis.call('ZADD', zsetBars, popedScore, cjson.encode(popedBar))
            redis.call('PUBLISH', topic, '{"type":"bar","resolution":"' .. barType .. '","sequenceId":' .. seqId .. ',"data":' .. cjson.encode(popedBar) .. '}')
        else
            -- 可持久化最后一个Bar，生成新的Bar:
            if popedScore < timestamp then
                redis.call('ZADD', zsetBars, popedScore, cjson.encode(popedBar), timestamp, cjson.encode(newBar))
                redis.call('PUBLISH', topic, '{"type":"bar","resolution":"' .. barType .. '","sequenceId":' .. seqId .. ',"data":' .. cjson.encode(newBar) .. '}')
                return popedBar
            end
        end
    end
    return nil
end

local seqId = ARGV[1]
local KEY_BAR_SEQ = '_BarSeq_'

local zsetBars, topics, barTypeStartTimes
local openPrice, highPrice, lowPrice, closePrice, quantity
local persistBars = {}

-- 检查sequence:
local seq = redis.call('GET', KEY_BAR_SEQ)
if not seq or tonumber(seqId) > tonumber(seq) then
    zsetBars = { KEYS[1], KEYS[2], KEYS[3], KEYS[4] }
    barTypeStartTimes = { tonumber(ARGV[2]), tonumber(ARGV[3]), tonumber(ARGV[4]), tonumber(ARGV[5]) }
    openPrice = tonumber(ARGV[6])
    highPrice = tonumber(ARGV[7])
    lowPrice = tonumber(ARGV[8])
    closePrice = tonumber(ARGV[9])
    quantity = tonumber(ARGV[10])

    local i, bar
    local names = { 'SEC', 'MIN', 'HOUR', 'DAY' }
    -- 检查是否可以merge:
    for i = 1, 4 do
        bar = tryMergeLast(names[i], seqId, zsetBars[i], barTypeStartTimes[i], { barTypeStartTimes[i], openPrice, highPrice, lowPrice, closePrice, quantity })
        if bar then
            persistBars[names[i]] = bar
        end
    end
    redis.call('SET', KEY_BAR_SEQ, seqId)
    return cjson.encode(persistBars)
end

redis.log(redis.LOG_WARNING, 'sequence ignored: exist seq => ' .. seq .. ' >= ' .. seqId .. ' <= new seq')

return '{}'
