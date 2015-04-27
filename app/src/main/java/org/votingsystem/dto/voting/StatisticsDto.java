package org.votingsystem.dto.voting;

import org.votingsystem.dto.UserVSDto;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.TypeVS;

import java.text.ParseException;
import java.util.Date;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class StatisticsDto {
	
	public static final String TAG = "StatisticsVS";
   
    private Long id;
    private EventVSDto.State state;
    private TypeVS typeVS;
    private UserVSDto userVS;
    private Integer numSignatures;
    private Integer numAccessRequests;
    private Integer numVotesVSAccounted;
    private String validatedPublishRequestURL;
    private String publishRequestURL;
    private String strDateBegin;
    private String strdateFinish;
    private Date dateBegin;
    private Date dateFinish;
    
    public void setState(EventVSDto.State state) {
        this.state = state;
    }
    public EventVSDto.State getState() {
        return state;
    }
    public void setUserVS(UserVSDto userVS) {
        this.userVS = userVS;
    }
    public UserVSDto getUserVS() {
        return userVS;
    }
    public void setNumSignatures(int numSignatures) {
        this.numSignatures = numSignatures;
    }
    public Integer getNumSignatures() {
        return numSignatures;
    }
    public void setStrDateBegin(String strDateBegin) throws ParseException {
        this.strDateBegin = strDateBegin;
        dateBegin = DateUtils.getDateFromString(strDateBegin);
    }
    public String getStrDateBegin() {
        return strDateBegin;
    }
    public void setStrdateFinish(String strdateFinish) throws ParseException {
        this.strdateFinish = strdateFinish;
        dateFinish = DateUtils.getDateFromString(strdateFinish);
    }
    public String getStrdateFinish() {
        return strdateFinish;
    }
    public void setDateBegin(Date dateBegin) {
        this.dateBegin = dateBegin;
    }
    public Date getDateBegin() {
            return dateBegin;
    }
    public void setDateFinish(Date dateFinish) {
        this.dateFinish = dateFinish;
    }
    public Date getDateFinish() {
        return dateFinish;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getId() {
        return id;
    }
    public TypeVS getTypeVS() {
        return typeVS;
    }
    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
    }

    /**
     * @return the numAccessRequests
     */
    public Integer getNumeroSolicitudesDeAcceso() {
        return numAccessRequests;
    }

    /**
     * @param numAccessRequests the numAccessRequests to set
     */
    public void setNumeroSolicitudesDeAcceso(Integer numAccessRequests) {
        this.numAccessRequests = numAccessRequests;
    }
	public String getSolicitudPublicacionValidadaURL() {
		return validatedPublishRequestURL;
	}
	public void setSolicitudPublicacionValidadaURL(
			String validatedPublishRequestURL) {
		this.validatedPublishRequestURL = validatedPublishRequestURL;
	}
	public String getSolicitudPublicacionURL() {
		return publishRequestURL;
	}
	public void setSolicitudPublicacionURL(String publishRequestURL) {
		this.publishRequestURL = publishRequestURL;
	}

    /**
     * @return the numVotesVSAccounted
     */
    public Integer getNumVotesVSAccounted() {
        return numVotesVSAccounted;
    }

    /**
     * @param numVotesVSAccounted the numVotesVSAccounted to set
     */
    public void setNumeroVotosContabilizados(Integer numVotesVSAccounted) {
        this.numVotesVSAccounted = numVotesVSAccounted;
    }

}
