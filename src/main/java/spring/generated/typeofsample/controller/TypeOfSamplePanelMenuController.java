package spring.generated.typeofsample.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import spring.generated.forms.TypeOfSamplePanelMenuForm;
import spring.mine.common.controller.BaseController;
import spring.mine.common.form.BaseForm;
import spring.mine.common.validator.BaseErrors;

//seemingly unused controller
@Controller
public class TypeOfSamplePanelMenuController extends BaseController {
	@RequestMapping(value = "/TypeOfSamplePanelMenu", method = RequestMethod.GET)
	public ModelAndView showTypeOfSamplePanelMenu(HttpServletRequest request,
			@ModelAttribute("form") TypeOfSamplePanelMenuForm form) {
		String forward = FWD_SUCCESS;
		if (form == null) {
			form = new TypeOfSamplePanelMenuForm();
		}
		form.setFormAction("");
		Errors errors = new BaseErrors();

		return findForward(forward, form);
	}

	@Override
	protected ModelAndView findLocalForward(String forward, BaseForm form) {
		if (FWD_SUCCESS.equals(forward)) {
			return new ModelAndView("haitiMasterListsPageDefinition", "form", form);
		} else if (FWD_FAIL.equals(forward)) {
			return new ModelAndView("/MasterListsPage.do", "form", form);
		} else {
			return new ModelAndView("PageNotFound");
		}
	}

	@Override
	protected String getPageTitleKey() {
		return null;
	}

	@Override
	protected String getPageSubtitleKey() {
		return null;
	}
}
