package spring.generated.userrole.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import spring.generated.forms.UserRoleForm;
import spring.mine.common.controller.BaseController;
import spring.mine.common.form.BaseForm;
import spring.mine.common.validator.BaseErrors;

//seemingly unused controller
@Controller
public class UpdateNextPreviousUserRoleController extends BaseController {
	@RequestMapping(value = "/UpdateNextPreviousUserRole", method = RequestMethod.GET)
	public ModelAndView showUpdateNextPreviousUserRole(HttpServletRequest request,
			@ModelAttribute("form") UserRoleForm form) {
		String forward = FWD_SUCCESS;
		if (form == null) {
			form = new UserRoleForm();
		}
		form.setFormAction("");
		Errors errors = new BaseErrors();

		return findForward(forward, form);
	}

	@Override
	protected ModelAndView findLocalForward(String forward, BaseForm form) {
		if (FWD_SUCCESS.equals(forward)) {
			return new ModelAndView("/UserRole.do", "form", form);
		} else if (FWD_FAIL.equals(forward)) {
			return new ModelAndView("userRoleDefinition", "form", form);
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
