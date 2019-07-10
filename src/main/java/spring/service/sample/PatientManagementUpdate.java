package spring.service.sample;

import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.validator.GenericValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;

import spring.mine.common.validator.BaseErrors;
import spring.mine.sample.form.SamplePatientEntryForm;
import spring.service.address.AddressPartService;
import spring.service.address.PersonAddressService;
import spring.service.patient.PatientService;
import spring.service.patientidentity.PatientIdentityService;
import spring.service.patienttype.PatientPatientTypeService;
import spring.service.person.PersonService;
import spring.service.search.SearchResultsService;
import us.mn.state.health.lims.address.valueholder.AddressPart;
import us.mn.state.health.lims.address.valueholder.PersonAddress;
import us.mn.state.health.lims.common.action.IActionConstants;
import us.mn.state.health.lims.common.exception.LIMSRuntimeException;
import us.mn.state.health.lims.common.provider.query.PatientSearchResults;
import us.mn.state.health.lims.common.util.ConfigurationProperties;
import us.mn.state.health.lims.login.valueholder.UserSessionData;
import us.mn.state.health.lims.patient.action.IPatientUpdate;
import us.mn.state.health.lims.patient.action.bean.PatientManagementInfo;
import us.mn.state.health.lims.patient.valueholder.Patient;
import us.mn.state.health.lims.patientidentity.valueholder.PatientIdentity;
import us.mn.state.health.lims.patientidentitytype.util.PatientIdentityTypeMap;
import us.mn.state.health.lims.patienttype.util.PatientTypeMap;
import us.mn.state.health.lims.patienttype.valueholder.PatientPatientType;
import us.mn.state.health.lims.person.valueholder.Person;

@Service
@Scope("prototype")
public class PatientManagementUpdate implements IPatientUpdate {

	private String currentUserId;
	protected Patient patient;
	protected Person person;
	private List<PatientIdentity> patientIdentities;
	private String patientID = "";
	@Autowired
	private PatientIdentityService identityService;
	@Autowired
	private PatientService patientService;
	@Autowired
	private PersonAddressService personAddressService;
	@Autowired
	private PersonService personService;
	@Autowired
	private AddressPartService addressPartService;
	@Autowired
	private PatientPatientTypeService patientPatientTypeService;
	@Autowired
	private SearchResultsService search;
	private final String AMBIGUOUS_DATE_CHAR = ConfigurationProperties.getInstance()
			.getPropertyValue(ConfigurationProperties.Property.AmbiguousDateHolder);
	private final String AMBIGUOUS_DATE_HOLDER = AMBIGUOUS_DATE_CHAR + AMBIGUOUS_DATE_CHAR;
	protected PatientUpdateStatus patientUpdateStatus = PatientUpdateStatus.NO_ACTION;

	private String ADDRESS_PART_VILLAGE_ID;
	private String ADDRESS_PART_COMMUNE_ID;
	private String ADDRESS_PART_DEPT_ID;

	@PostConstruct
	public void initializeGlobalVariables() {
		List<AddressPart> partList = addressPartService.getAll();
		for (AddressPart addressPart : partList) {
			if ("department".equals(addressPart.getPartName())) {
				ADDRESS_PART_DEPT_ID = addressPart.getId();
			} else if ("commune".equals(addressPart.getPartName())) {
				ADDRESS_PART_COMMUNE_ID = addressPart.getId();
			} else if ("village".equals(addressPart.getPartName())) {
				ADDRESS_PART_VILLAGE_ID = addressPart.getId();
			}
		}

	}

	protected String getSysUserId(HttpServletRequest request) {
		UserSessionData usd = (UserSessionData) request.getSession().getAttribute(IActionConstants.USER_SESSION_DATA);
		return String.valueOf(usd.getSystemUserId());
	}

	public void setSysUserIdFromRequest(HttpServletRequest request) {
		UserSessionData usd = (UserSessionData) request.getSession().getAttribute(IActionConstants.USER_SESSION_DATA);
		currentUserId = String.valueOf(usd.getSystemUserId());
	}

	private Errors validatePatientInfo(PatientManagementInfo patientInfo) {
		Errors errors = new BaseErrors();
		if (ConfigurationProperties.getInstance()
				.isPropertyValueEqual(ConfigurationProperties.Property.ALLOW_DUPLICATE_SUBJECT_NUMBERS, "false")) {
			String newSTNumber = GenericValidator.isBlankOrNull(patientInfo.getSTnumber()) ? null
					: patientInfo.getSTnumber();
			String newSubjectNumber = GenericValidator.isBlankOrNull(patientInfo.getSubjectNumber()) ? null
					: patientInfo.getSubjectNumber();
			String newNationalId = GenericValidator.isBlankOrNull(patientInfo.getNationalId()) ? null
					: patientInfo.getNationalId();

			List<PatientSearchResults> results = search.getSearchResults(null, null, newSTNumber, newSubjectNumber,
					newNationalId, null, null, null);

			if (!results.isEmpty()) {

				for (PatientSearchResults result : results) {
					if (!result.getPatientID().equals(patientInfo.getPatientPK())) {
						if (newSTNumber != null && newSTNumber.equals(result.getSTNumber())) {
							errors.reject("error.duplicate.STNumber", null, null);
						}
						if (newSubjectNumber != null && newSubjectNumber.equals(result.getSubjectNumber())) {
							errors.reject("error.duplicate.subjectNumber", null, null);
						}
						if (newNationalId != null && newNationalId.equals(result.getNationalId())) {
							errors.reject("error.duplicate.nationalId", null, null);
						}
					}
				}
			}
		}

		validateBirthdateFormat(patientInfo, errors);

		return errors;
	}

	private void initMembers() {
		patient = new Patient();
		person = new Person();
		patientIdentities = new ArrayList<>();
	}

	private void loadForUpdate(PatientManagementInfo patientInfo) {

		patientID = patientInfo.getPatientPK();
		patient = patientService.readPatient(patientID);
		person = patient.getPerson();

		patientIdentities = identityService.getPatientIdentitiesForPatient(patient.getId());
	}

	private void validateBirthdateFormat(PatientManagementInfo patientInfo, Errors errors) {
		String birthDate = patientInfo.getBirthDateForDisplay();
		boolean validBirthDateFormat = true;

		if (!GenericValidator.isBlankOrNull(birthDate)) {
			validBirthDateFormat = birthDate.length() == 10;
			// the regex matches ambiguous day and month or ambiguous day or completely
			// formed date
			if (validBirthDateFormat) {
				validBirthDateFormat = birthDate.matches("(((" + AMBIGUOUS_DATE_HOLDER + "|\\d{2})/\\d{2})|"
						+ AMBIGUOUS_DATE_HOLDER + "/(" + AMBIGUOUS_DATE_HOLDER + "|\\d{2}))/\\d{4}");
			}

			if (!validBirthDateFormat) {
				errors.reject("error.birthdate.format", null, null);
			}
		}
	}

	private void copyFormBeanToValueHolders(PatientManagementInfo patientInfo)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

		PropertyUtils.copyProperties(patient, patientInfo);
		PropertyUtils.copyProperties(person, patientInfo);
	}

	private void setSystemUserID(String currentUserId) {
		patient.setSysUserId(currentUserId);
		person.setSysUserId(currentUserId);

		for (PatientIdentity identity : patientIdentities) {
			identity.setSysUserId(currentUserId);
		}
	}

	private void setLastUpdatedTimeStamps(PatientManagementInfo patientInfo) {
		String patientUpdate = patientInfo.getPatientLastUpdated();
		if (!GenericValidator.isBlankOrNull(patientUpdate)) {
			Timestamp timeStamp = Timestamp.valueOf(patientUpdate);
			patient.setLastupdated(timeStamp);
		}

		String personUpdate = patientInfo.getPersonLastUpdated();
		if (!GenericValidator.isBlankOrNull(personUpdate)) {
			Timestamp timeStamp = Timestamp.valueOf(personUpdate);
			person.setLastupdated(timeStamp);
		}
	}

	protected void persistPatientRelatedInformation(PatientManagementInfo patientInfo) {
		persistIdentityTypes(patientInfo);
		persistExtraPatientAddressInfo(patientInfo);
		persistPatientType(patientInfo);
	}

	protected void persistIdentityTypes(PatientManagementInfo patientInfo) {

		persistIdentityType(patientInfo.getSTnumber(), "ST");
		persistIdentityType(patientInfo.getMothersName(), "MOTHER");
		persistIdentityType(patientInfo.getAka(), "AKA");
		persistIdentityType(patientInfo.getInsuranceNumber(), "INSURANCE");
		persistIdentityType(patientInfo.getOccupation(), "OCCUPATION");
		persistIdentityType(patientInfo.getSubjectNumber(), "SUBJECT");
		persistIdentityType(patientInfo.getMothersInitial(), "MOTHERS_INITIAL");
		persistIdentityType(patientInfo.getEducation(), "EDUCATION");
		persistIdentityType(patientInfo.getMaritialStatus(), "MARITIAL");
		persistIdentityType(patientInfo.getNationality(), "NATIONALITY");
		persistIdentityType(patientInfo.getHealthDistrict(), "HEALTH DISTRICT");
		persistIdentityType(patientInfo.getHealthRegion(), "HEALTH REGION");
		persistIdentityType(patientInfo.getOtherNationality(), "OTHER NATIONALITY");
	}

	private void persistExtraPatientAddressInfo(PatientManagementInfo patientInfo) {
		PersonAddress village = null;
		PersonAddress commune = null;
		PersonAddress dept = null;
		List<PersonAddress> personAddressList = personAddressService.getAddressPartsByPersonId(person.getId());

		for (PersonAddress address : personAddressList) {
			if (address.getAddressPartId().equals(ADDRESS_PART_COMMUNE_ID)) {
				commune = address;
				commune.setValue(patientInfo.getCommune());
				commune.setSysUserId(currentUserId);
				personAddressService.update(commune);
			} else if (address.getAddressPartId().equals(ADDRESS_PART_VILLAGE_ID)) {
				village = address;
				village.setValue(patientInfo.getCity());
				village.setSysUserId(currentUserId);
				personAddressService.update(village);
			} else if (address.getAddressPartId().equals(ADDRESS_PART_DEPT_ID)) {
				dept = address;
				if (!GenericValidator.isBlankOrNull(patientInfo.getAddressDepartment())
						&& !patientInfo.getAddressDepartment().equals("0")) {
					dept.setValue(patientInfo.getAddressDepartment());
					dept.setType("D");
					dept.setSysUserId(currentUserId);
					personAddressService.update(dept);
				}
			}
		}

		if (commune == null) {
			insertNewPatientInfo(ADDRESS_PART_COMMUNE_ID, patientInfo.getCommune(), "T");
		}

		if (village == null) {
			insertNewPatientInfo(ADDRESS_PART_VILLAGE_ID, patientInfo.getCity(), "T");
		}

		if (dept == null && patientInfo.getAddressDepartment() != null
				&& !patientInfo.getAddressDepartment().equals("0")) {
			insertNewPatientInfo(ADDRESS_PART_DEPT_ID, patientInfo.getAddressDepartment(), "D");
		}

	}

	private void insertNewPatientInfo(String partId, String value, String type) {
		PersonAddress address;
		address = new PersonAddress();
		address.setPersonId(person.getId());
		address.setAddressPartId(partId);
		address.setType(type);
		address.setValue(value);
		address.setSysUserId(currentUserId);
		personAddressService.insert(address);
	}

	public void persistIdentityType(String paramValue, String type) throws LIMSRuntimeException {

		Boolean newIdentityNeeded = true;
		String typeID = PatientIdentityTypeMap.getInstance().getIDForType(type);

		if (patientUpdateStatus == PatientUpdateStatus.UPDATE) {

			for (PatientIdentity listIdentity : patientIdentities) {
				if (listIdentity.getIdentityTypeId().equals(typeID)) {

					newIdentityNeeded = false;

					if ((listIdentity.getIdentityData() == null && !GenericValidator.isBlankOrNull(paramValue))
							|| (listIdentity.getIdentityData() != null
							&& !listIdentity.getIdentityData().equals(paramValue))) {
						listIdentity.setIdentityData(paramValue);
						identityService.update(listIdentity);
					}

					break;
				}
			}
		}

		if (newIdentityNeeded && !GenericValidator.isBlankOrNull(paramValue)) {
			// either a new patient or a new identity item
			PatientIdentity identity = new PatientIdentity();
			identity.setPatientId(patient.getId());
			identity.setIdentityTypeId(typeID);
			identity.setSysUserId(currentUserId);
			identity.setIdentityData(paramValue);
			identity.setLastupdatedFields();
			identityService.insert(identity);
		}
	}

	protected void persistPatientType(PatientManagementInfo patientInfo) {

		String typeName = null;

		try {
			typeName = patientInfo.getPatientType();
		} catch (Exception ignored) {
			System.out.println("typeName ignored");
		}

		if (!GenericValidator.isBlankOrNull(typeName) && !"0".equals(typeName)) {
			String typeID = PatientTypeMap.getInstance().getIDForType(typeName);

			PatientPatientType patientPatientType = patientPatientTypeService
					.getPatientPatientTypeForPatient(patient.getId());

			if (patientPatientType == null) {
				patientPatientType = new PatientPatientType();
				patientPatientType.setSysUserId(currentUserId);
				patientPatientType.setPatientId(patient.getId());
				patientPatientType.setPatientTypeId(typeID);
				patientPatientTypeService.insert(patientPatientType);
			} else {
				patientPatientType.setSysUserId(currentUserId);
				patientPatientType.setPatientTypeId(typeID);
				patientPatientTypeService.update(patientPatientType);
			}
		}
	}

	@Override
	public Errors preparePatientData(HttpServletRequest request, PatientManagementInfo patientInfo)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Errors errors = validatePatientInfo(patientInfo);
		if (errors.hasErrors()) {
			return errors;
		}

		initMembers();

		if (patientUpdateStatus == PatientUpdateStatus.UPDATE) {
			loadForUpdate(patientInfo);
		}

		copyFormBeanToValueHolders(patientInfo);

		setSystemUserID(getSysUserId(request));

		setLastUpdatedTimeStamps(patientInfo);

		return errors;
	}

	@Override
	public void setPatientUpdateStatus(PatientManagementInfo patientInfo) {
		patientUpdateStatus = patientInfo.getPatientUpdateStatus();
		/*
		 * String status = patientInfo.getPatientProcessingStatus();
		 *
		 * if ("noAction".equals(status)) { patientUpdateStatus =
		 * PatientUpdateStatus.NO_ACTION; } else if ("update".equals(status)) {
		 * patientUpdateStatus = PatientUpdateStatus.UPDATE; } else {
		 * patientUpdateStatus = PatientUpdateStatus.ADD; }
		 */
	}

	@Override
	public PatientUpdateStatus getPatientUpdateStatus() {
		return patientUpdateStatus;
	}

	@Override
	public void persistPatientData(PatientManagementInfo patientInfo) throws LIMSRuntimeException {

		if (patientUpdateStatus == PatientUpdateStatus.ADD) {
			personService.insert(person);
		} else if (patientUpdateStatus == PatientUpdateStatus.UPDATE) {
			personService.update(person);
		}
		patient.setPerson(person);

		if (patientUpdateStatus == PatientUpdateStatus.ADD) {
			patientService.insert(patient);
		} else if (patientUpdateStatus == PatientUpdateStatus.UPDATE) {
			patientService.update(patient);
		}

		persistPatientRelatedInformation(patientInfo);
		patientID = patient.getId();

	}

	@Override
	public String getPatientId(SamplePatientEntryForm form) {
		return GenericValidator.isBlankOrNull(patientID) ? form.getPatientProperties().getPatientPK() : patientID;
	}

}