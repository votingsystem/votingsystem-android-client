package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.votingsystem.util.OperationType;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResultListDto<T> {

    private List<T> resultList;
    private Integer offset;
    private Integer max;
    private Long totalCount;
    private Object State;
    private String message;
    private OperationType type;

    public ResultListDto() { }

    public ResultListDto(List<T> resultList) {
        this(resultList, 0, resultList.size(), Long.valueOf(resultList.size()));
    }

    public ResultListDto(List<T> resultList, OperationType type) {
        this(resultList, 0, resultList.size(), Long.valueOf(resultList.size()));
        this.type = type;
    }

    public ResultListDto(List<T> resultList, Integer offset, Integer max, Long totalCount) {
        this.resultList = resultList;
        this.offset = offset;
        this.max = max;
        this.totalCount = totalCount;
    }

    public ResultListDto(List<T> resultList, Integer offset, Integer max, Integer totalCount) {
        this(resultList, offset, max, Long.valueOf(totalCount));
    }

    public static <T> ResultListDto GROUP(List<T> groupList, Object state, Integer offset, Integer max, Long totalCount) {
        ResultListDto<T> result = new ResultListDto<T>();
        result.setResultList(groupList);
        result.setState(state);
        result.setOffset(offset);
        result.setMax(max);
        result.setTotalCount(totalCount);
        return result;
    }

    public List<T> getResultList() {
        return resultList;
    }

    public void setResultList(List<T> resultList) {
        this.resultList = resultList;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Object getState() {
        return State;
    }

    public void setState(Object state) {
        State = state;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OperationType getType() {
        return type;
    }

    public void setType(OperationType type) {
        this.type = type;
    }
}
