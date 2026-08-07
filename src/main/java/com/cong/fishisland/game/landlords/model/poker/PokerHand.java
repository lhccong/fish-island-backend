package com.cong.fishisland.game.landlords.model.poker;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 手牌
 *
 * @author cong
 */
@Getter
public class PokerHand {
    
    /**
     * 手牌列表
     */
    private List<Poker> pokers;

    public PokerHand() {
        this.pokers = new ArrayList<>();
    }

    public PokerHand(List<Poker> pokers) {
        this.pokers = pokers != null ? new ArrayList<>(pokers) : new ArrayList<>();
    }

    /**
     * 添加牌
     */
    public void add(Poker poker) {
        if (poker != null) {
            this.pokers.add(poker);
        }
    }

    /**
     * 添加牌组
     */
    public void addAll(List<Poker> pokers) {
        if (pokers != null) {
            this.pokers.addAll(pokers);
        }
    }

    /**
     * 移除牌
     */
    public boolean remove(Poker poker) {
        return this.pokers.remove(poker);
    }

    /**
     * 移除指定索引的牌
     */
    public Poker remove(int index) {
        if (index >= 0 && index < pokers.size()) {
            return pokers.remove(index);
        }
        return null;
    }

    /**
     * 清空手牌
     */
    public void clear() {
        this.pokers.clear();
    }

    /**
     * 获取牌数量
     */
    public int size() {
        return pokers.size();
    }

    /**
     * 是否为空
     */
    @JSONField(serialize = false)
    public boolean isEmpty() {
        return pokers.isEmpty();
    }

    /**
     * 获取所有牌
     */
    public List<Poker> getAll() {
        return new ArrayList<>(pokers);
    }

    /**
     * 排序 (按面值升序)
     */
    public void sortByValue() {
        Collections.sort(pokers, (a, b) -> a.getLandlordsSortValue() - b.getLandlordsSortValue());
    }

    /**
     * 排序 (按面值降序)
     */
    public void sortByValueDesc() {
        Collections.sort(pokers, (a, b) -> b.getLandlordsSortValue() - a.getLandlordsSortValue());
    }

    /**
     * 获取指定面值的牌
     */
    public List<Poker> getByValue(int value) {
        return pokers.stream()
                .filter(p -> p.getValue().getValue() == value)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定面值的牌数量
     */
    public int countByValue(int value) {
        return (int) pokers.stream()
                .filter(p -> p.getValue().getValue() == value)
                .count();
    }

    /**
     * 获取指定面值的牌数量 (考虑癞子)
     */
    public int countByValueWithUniversal(int value, int universalValue) {
        return countByValue(value) + countByValue(universalValue);
    }

    /**
     * 按面值分组
     */
    public Map<Integer, List<Poker>> groupByValue() {
        return pokers.stream()
                .collect(Collectors.groupingBy(p -> p.getValue().getValue()));
    }

    /**
     * 获取癞子牌
     */
    @JSONField(serialize = false)
    public List<Poker> getUniversals() {
        return pokers.stream()
                .filter(Poker::isUniversal)
                .collect(Collectors.toList());
    }

    /**
     * 获取癞子数量
     */
    @JSONField(serialize = false)
    public int getUniversalCount() {
        return (int) pokers.stream()
                .filter(Poker::isUniversal)
                .count();
    }

    /**
     * 获取非癞子牌
     */
    @JSONField(serialize = false)
    public List<Poker> getNonUniversals() {
        return pokers.stream()
                .filter(p -> !p.isUniversal())
                .collect(Collectors.toList());
    }

    /**
     * 检查是否有指定面值的牌
     */
    public boolean hasValue(int value) {
        return pokers.stream()
                .anyMatch(p -> p.getValue().getValue() == value);
    }

    /**
     * 检查是否有癞子
     */
    public boolean hasUniversal() {
        return pokers.stream()
                .anyMatch(p -> p.isUniversal());
    }

    /**
     * 获取最大面值
     */
    @JSONField(serialize = false)
    public int getMaxValue() {
        return pokers.stream()
                .mapToInt(p -> p.getLandlordsSortValue())
                .max()
                .orElse(0);
    }

    /**
     * 转换为 ID 列表 (用于传输)
     */
    @JSONField(serialize = false)
    public List<String> toIdList() {
        return pokers.stream()
                .map(p -> p.getId())
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return pokers.stream()
                .map(p -> p.getDisplayName())
                .collect(Collectors.joining(" "));
    }
}
