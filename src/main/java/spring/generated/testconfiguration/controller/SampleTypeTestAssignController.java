package spring.generated.testconfiguration.controller;

import java.lang.String;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import spring.generated.forms.SampleTypeTestAssignForm;
import spring.mine.common.controller.BaseController;
import spring.mine.common.form.BaseForm;
import spring.mine.common.validator.BaseErrors;
import us.mn.state.health.lims.common.services.DisplayListService;
import us.mn.state.health.lims.common.services.TestService;
import us.mn.state.health.lims.common.services.TypeOfSampleService;
import us.mn.state.health.lims.common.util.IdValuePair;
import us.mn.state.health.lims.common.util.validator.GenericValidator;
import us.mn.state.health.lims.hibernate.HibernateUtil;
import us.mn.state.health.lims.test.valueholder.Test;
import us.mn.state.health.lims.typeofsample.daoimpl.TypeOfSampleDAOImpl;
import us.mn.state.health.lims.typeofsample.daoimpl.TypeOfSampleTestDAOImpl;
import us.mn.state.health.lims.typeofsample.valueholder.TypeOfSample;
import us.mn.state.health.lims.typeofsample.valueholder.TypeOfSampleTest;

@Controller
public class SampleTypeTestAssignController extends BaseController {
  @RequestMapping(
      value = "/SampleTypeTestAssign",
      method = RequestMethod.GET
  )
  public ModelAndView showSampleTypeTestAssign(HttpServletRequest request,
      @ModelAttribute("form") SampleTypeTestAssignForm form) {
    String forward = FWD_SUCCESS;
    if (form == null) {
    	form = new SampleTypeTestAssignForm();
    }
        form.setFormAction("");
    BaseErrors errors = new BaseErrors();
    if (form.getErrors() != null) {
    	errors = (BaseErrors) form.getErrors();
    }
    ModelAndView mv = checkUserAndSetup(form, errors, request);

    if (errors.hasErrors()) {
    	return mv;
    }
    
    List<IdValuePair> typeOfSamples = DisplayListService.getListWithLeadingBlank(DisplayListService.ListType.SAMPLE_TYPE);
    LinkedHashMap<IdValuePair, List<IdValuePair>> sampleTypesTestsMap = new LinkedHashMap<IdValuePair, List<IdValuePair>>(typeOfSamples.size());

    for( IdValuePair sampleTypePair : typeOfSamples){
        List<IdValuePair> tests = new ArrayList<IdValuePair>();
        sampleTypesTestsMap.put(sampleTypePair, tests );
        List<Test> testList = TypeOfSampleService.getAllTestsBySampleTypeId(sampleTypePair.getId());

        for( Test test : testList){
            if( test.isActive()) {
                tests.add(new IdValuePair(test.getId(), TestService.getLocalizedTestNameWithType(test)));
            }
        }
    }

    //we can't just append the original list because that list is in the cache
    List<IdValuePair> joinedList = new ArrayList<IdValuePair>(typeOfSamples);
    joinedList.addAll(DisplayListService.getList(DisplayListService.ListType.SAMPLE_TYPE_INACTIVE));
    try {
    	PropertyUtils.setProperty(form, "sampleTypeList", joinedList);
		PropertyUtils.setProperty(form, "sampleTypeTestList", sampleTypesTestsMap);
	} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

    return findForward(forward, form);
  }
  
  protected ModelAndView findLocalForward(String forward, BaseForm form) {
    if ("success".equals(forward)) {
      return new ModelAndView("sampleTypeAssignDefinition", "form", form);
    } else if (FWD_SUCCESS_INSERT.equals(forward)) {
        return new ModelAndView("redirect:/SampleTypeTestAssign.do", "form", form);
    } else {
      return new ModelAndView("PageNotFound");
    }
  }

  protected String getPageTitleKey() {
    return null;
  }

  protected String getPageSubtitleKey() {
    return null;
  }
  
  @RequestMapping(
	      value = "/SampleTypeTestAssign",
	      method = RequestMethod.POST
	  )
	  public ModelAndView postSampleTypeTestAssign(HttpServletRequest request,
	      @ModelAttribute("form") SampleTypeTestAssignForm form) {
	    String forward = FWD_SUCCESS_INSERT;
	    if (form == null) {
	    	form = new SampleTypeTestAssignForm();
	    }
	        form.setFormAction("");
	    BaseErrors errors = new BaseErrors();
	    if (form.getErrors() != null) {
	    	errors = (BaseErrors) form.getErrors();
	    }
	    ModelAndView mv = checkUserAndSetup(form, errors, request);

	    if (errors.hasErrors()) {
	    	return mv;
	    }
	    
        String testId = form.getString("testId");
        String sampleTypeId = form.getString("sampleTypeId");
        String deactivateSampleTypeId = form.getString("deactivateSampleTypeId");
        boolean updateTypeOfSample = false;
        String currentUser = getSysUserId(request);
        
        TypeOfSample typeOfSample = TypeOfSampleService.getTransientTypeOfSampleById(sampleTypeId);
        TypeOfSample deActivateTypeOfSample = null;
        
        //Test test = new TestService(testId).getTest();
        

        //This covers the case that they are moving the test to the same sample type they are moving it from
        if(sampleTypeId.equals(deactivateSampleTypeId)){
            return findForward(FWD_SUCCESS_INSERT, form);
        }

        TypeOfSampleTest typeOfSampleTestOld = new TypeOfSampleTestDAOImpl().getTypeOfSampleTestForTest(testId);
        boolean deleteExistingTypeOfSampleTest = false;
        String[] typeOfSamplesTestIDs = new String[1];
        
        if (typeOfSampleTestOld != null) {       	
        	typeOfSamplesTestIDs[0] = typeOfSampleTestOld.getId();
        	deleteExistingTypeOfSampleTest = true;
        }
    //---------------------------    
       /* if( "N".equals(typeOfSample.getIsActive())){
        	typeOfSample.setIsActive(true);
        	typeOfSample.setSysUserId(currentUser);
        	updateTypeOfSample = true;
        }*/
        
        
      // Boolean value = false; 
        if( typeOfSample.getIsActive()== false){
        	typeOfSample.setIsActive(true);
        	typeOfSample.setSysUserId(currentUser);
        	updateTypeOfSample = true;
        }
        
        
        
//------------------------------------------
        if( !GenericValidator.isBlankOrNull(deactivateSampleTypeId) ){
        	deActivateTypeOfSample  = TypeOfSampleService.getTransientTypeOfSampleById(deactivateSampleTypeId);
        	deActivateTypeOfSample.setIsActive(false);
        	deActivateTypeOfSample.setSysUserId(currentUser);
        }

        Transaction tx = HibernateUtil.getSession().beginTransaction();
        try {
        	if (deleteExistingTypeOfSampleTest) {
        		new TypeOfSampleTestDAOImpl().deleteData(typeOfSamplesTestIDs, currentUser);
        	}

            if(updateTypeOfSample){
                new TypeOfSampleDAOImpl().updateData(typeOfSample);
            }            
            
            TypeOfSampleTest typeOfSampleTest = new TypeOfSampleTest();
            typeOfSampleTest.setTestId(testId);
            typeOfSampleTest.setTypeOfSampleId(sampleTypeId);
            typeOfSampleTest.setSysUserId(currentUser);
            typeOfSampleTest.setLastupdatedFields();
            
            new TypeOfSampleTestDAOImpl().insertData(typeOfSampleTest);
        
            if( deActivateTypeOfSample != null){
                new TypeOfSampleDAOImpl().updateData(deActivateTypeOfSample);
            }
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
        } finally {
            HibernateUtil.closeSession();
        }

        DisplayListService.refreshList(DisplayListService.ListType.SAMPLE_TYPE);
        DisplayListService.refreshList(DisplayListService.ListType.SAMPLE_TYPE_INACTIVE);
	    
	    
	    return findForward(forward, form);
  }
}

