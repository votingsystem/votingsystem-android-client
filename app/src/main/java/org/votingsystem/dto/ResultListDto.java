package org.votingsystem.dto;

import org.votingsystem.util.OperationType;

import java.io.Serializable;
import java.util.Set;

public class ResultListDto<T> implements Serializable {

    public static final long serialVersionUID = 1L;

    private Set<T> resultList;
    private Integer offset;
    private Integer statusCode;
    private Integer max;
    private Long totalCount;
    private Object State;
    private String message;
    private String base64Data;
    private OperationType type;

    public ResultListDto() {
    }

    public ResultListDto(Set<T> resultList) {
        this(resultList, 0, resultList.size(), Long.valueOf(resultList.size()));
    }

    public ResultListDto(Set<T> resultList, OperationType type) {
        this(resultList, 0, resultList.size(), Long.valueOf(resultList.size()));
        this.type = type;
    }

    public ResultListDto(Set<T> resultList, Integer offset, Integer max, Long totalCount) {
        this.resultList = resultList;
        this.offset = offset;
        this.max = max;
        this.totalCount = totalCount;
    }

    public ResultListDto(Set<T> resultList, Integer offset, Integer max, Integer totalCount) {
        this(resultList, offset, max, Long.valueOf(totalCount));
    }

    public static <T> ResultListDto GROUP(Set<T> groupList, Object state, Integer offset,
                                          Integer max, Long totalCount) {
        ResultListDto<T> result = new ResultListDto<T>();
        result.setResultList(groupList);
        result.setState(state);
        result.setOffset(offset);
        result.setMax(max);
        result.setTotalCount(totalCount);
        return result;
    }

    public Set<T> getResultList() {
        return resultList;
    }

    public void setResultList(Set<T> resultList) {
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

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getBase64Data() {
        return base64Data;
    }

    public void setBase64Data(String base64Data) {
        this.base64Data = base64Data;
    }
}
