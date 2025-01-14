package org.openelisglobal.common.provider.query;

import org.openelisglobal.common.form.IPagingForm;
import org.openelisglobal.common.paging.PagingBean;

import java.util.List;

public class PatientSearchResultsForm implements IPagingForm {

    private PagingBean paging;

    private List<PatientSearchResults> patientSearchResults;

    public List<PatientSearchResults> getPatientSearchResults() {
        return patientSearchResults;
    }

    public void setPatientSearchResults(List<PatientSearchResults> patientSearchResults) {
        this.patientSearchResults = patientSearchResults;
    }

    @Override
    public void setPaging(PagingBean pagingBean) {
        this.paging = pagingBean;
    }

    @Override
    public PagingBean getPaging() {
        return paging;
    }
}
