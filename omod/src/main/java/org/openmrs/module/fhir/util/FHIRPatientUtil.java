package org.openmrs.module.fhir.util;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.dstu.composite.*;
import ca.uhn.fhir.model.dstu.resource.Observation;
import ca.uhn.fhir.model.dstu.resource.Patient;
import ca.uhn.fhir.model.dstu.valueset.*;
import ca.uhn.fhir.model.primitive.BooleanDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.hl7v2.model.v21.segment.ADD;
import org.openmrs.*;
import org.openmrs.api.context.Context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by snkasthu on 9/6/14.
 */
public class FHIRPatientUtil {

    /*public static void generatePatient(DelegatingResourceDescription description){
        Map<String, DelegatingResourceDescription.Property> map = description.getProperties();

        DelegatingResourceDescription.Property value = map.get("person");


    }*/

    public static String generatePatient(org.openmrs.Patient omrsPatient, String contentType) {
        Patient patient = new Patient();
        System.out.println("in get message");
        IdDt uuid = new IdDt();
        uuid.setValue(omrsPatient.getUuid());
        patient.setId(uuid);


        for (PatientIdentifier identifier : omrsPatient.getIdentifiers()) {
            String uri = Context.getAdministrationService().getSystemVariables().get("OPENMRS_HOSTNAME") + "/ws/rest/v1/patientidentifiertype/" + identifier.getIdentifierType().getUuid();
            if (identifier.isPreferred()) {

                patient.addIdentifier().setUse(IdentifierUseEnum.USUAL).setSystem(uri).setValue(identifier.getIdentifier()).setLabel(identifier.getIdentifierType().getName());
            } else {
                patient.addIdentifier().setUse(IdentifierUseEnum.SECONDARY).setSystem(uri).setValue(identifier.getIdentifier()).setLabel(identifier.getIdentifierType().getName());
            }
        }

        List<HumanNameDt> humanNameDts = new ArrayList<HumanNameDt>();

        System.out.print(omrsPatient.getNames().size());

        for (PersonName name : omrsPatient.getNames()) {

            HumanNameDt fhirName = new HumanNameDt();
            StringDt familyName = new StringDt();
            familyName.setValue(name.getFamilyName());
            List<StringDt> familyNames = new ArrayList<StringDt>();
            familyNames.add(familyName);

            fhirName.setFamily(familyNames);


            StringDt givenName = new StringDt();
            givenName.setValue(name.getGivenName());
            List<StringDt> givenNames = new ArrayList<StringDt>();
            givenNames.add(givenName);

            fhirName.setGiven(givenNames);


            if (name.getFamilyNameSuffix() != null) {
                StringDt suffix = fhirName.addSuffix();
                suffix.setValue(name.getFamilyNameSuffix());
            }

            if (name.getPrefix() != null) {
                StringDt prefix = fhirName.addPrefix();
                prefix.setValue(name.getPrefix());
            }


            if (name.isPreferred())
                fhirName.setUse(NameUseEnum.USUAL);
            else
                fhirName.setUse(NameUseEnum.NICKNAME);

            humanNameDts.add(fhirName);

        }

        patient.setName(humanNameDts);


        if (omrsPatient.getGender().equals("M"))
            patient.setGender(AdministrativeGenderCodesEnum.M);
        if (omrsPatient.getGender().equals("F"))
            patient.setGender(AdministrativeGenderCodesEnum.F);

        List<AddressDt> fhirAddresses = patient.getAddress();

        for (PersonAddress address : omrsPatient.getAddresses()) {

            AddressDt fhirAddress = new AddressDt();
            fhirAddress.setCity(address.getCityVillage());
            fhirAddress.setCountry(address.getCountry());

            List<StringDt> addressStrings = new ArrayList<StringDt>();

            addressStrings.add(new StringDt(address.getAddress1()));
            addressStrings.add(new StringDt(address.getAddress2()));
            addressStrings.add(new StringDt(address.getAddress3()));
            addressStrings.add(new StringDt(address.getAddress4()));
            addressStrings.add(new StringDt(address.getAddress5()));


            fhirAddress.setLine(addressStrings);

            if (address.isPreferred())
                fhirAddress.setUse(AddressUseEnum.HOME);
            else
                fhirAddress.setUse(AddressUseEnum.OLD);

            fhirAddresses.add(fhirAddress);

        }

        NarrativeDt dt = new NarrativeDt();

        patient.setText(dt);
        patient.setAddress(fhirAddresses);

        DateTimeDt fhirBirthDate = patient.getBirthDate();
        fhirBirthDate.setValue(omrsPatient.getBirthdate());

        patient.setActive(!omrsPatient.isVoided());

        BooleanDt isDeceased = new BooleanDt();
        isDeceased.setValue(omrsPatient.getDead());
        patient.setDeceased(isDeceased);

        List<ContactDt> dts = new ArrayList<ContactDt>();

        ContactDt telecom = new ContactDt();
        if (omrsPatient.getAttribute("Telephone Number") != null)
            telecom.setValue(omrsPatient.getAttribute("Telephone Number").getValue());
        else
            telecom.setValue("None");
        dts.add(telecom);
        patient.setTelecom(dts);

        FhirContext ctx = new FhirContext();
        // ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());

        IParser jsonParser = ctx.newJsonParser();
        IParser xmlParser = ctx.newXmlParser();

        String encoded = null;

        if(contentType.equals("application/xml+fhir")) {

            xmlParser.setPrettyPrint(true);
             encoded = xmlParser.encodeResourceToString(patient);
            System.out.println(encoded);
        }

        else {

            jsonParser.setPrettyPrint(true);
            encoded = jsonParser.encodeResourceToString(patient);
            System.out.println(encoded);
        }

        return encoded;

    }


    public static String generateObs(Obs obs) {

        Observation observation = new Observation();
        observation.setId(obs.getUuid());

        Collection<ConceptMap> mappings = obs.getConcept().getConceptMappings();
        CodeableConceptDt dt = observation.getName();
        List<CodingDt> dts = new ArrayList<CodingDt>();

        for (ConceptMap map : mappings) {
            System.out.println(map.getSource());
            System.out.println(map.getSource().getName());
            if (map.getSource().getName().equals("LOINC"))
                dts.add(new CodingDt().setCode("code").setDisplay("display").setSystem(Constants.loinc));
            if (map.getSource().getName().equals("SNOMED"))
                dts.add(new CodingDt().setCode("code").setDisplay("display").setSystem(Constants.snomed));
            if (map.getSource().getName().equals("CIEL"))
                dts.add(new CodingDt().setCode("code").setDisplay("display").setSystem(Constants.ciel));

        }
        dt.setCoding(dts);

        if (obs.getConcept().getDatatype().getHl7Abbreviation().equals("NM")) {

            obs.getConcept().isNumeric();
            ((ConceptNumeric)obs.getConcept()).getHiCritical();

            QuantityDt q = new QuantityDt();

            q.setValue(obs.getValueNumeric());

            q.setSystem("http://unitsofmeasure.org");

           // q.setCode(obs.get);

            observation.getReferenceRangeFirstRep().setLow(100);

            observation.getReferenceRangeFirstRep().setHigh(200);

            observation.setValue(q);

        }

        if (obs.getConcept().getDatatype().getHl7Abbreviation().equals("ST")) {
            StringDt value = new StringDt();
            value.setValue(obs.getValueAsString(Context.getLocale()));
            observation.setValue(value);

        }

        if (obs.getConcept().getDatatype().getHl7Abbreviation().equals("BIT")) {
            BooleanDt value = new BooleanDt();
            value.setValue(obs.getValueBoolean());
            observation.setValue(value);

        }

        if (obs.getConcept().getDatatype().getHl7Abbreviation().equals("CWE")) {

            Collection<ConceptMap> valueMappings = obs.getValueCoded().getConceptMappings();

            List<CodingDt> values = new ArrayList<CodingDt>();

            for (ConceptMap map : valueMappings) {

               map.getConceptReferenceTerm().getName();
                System.out.println(map.getSource().getName());
                if (map.getSource().getName().equals("LOINC"))
                    values.add(new CodingDt().setCode("code").setDisplay("display").setSystem(Constants.loinc));
                if (map.getSource().getName().equals("SNOMED"))
                    values.add(new CodingDt().setCode("code").setDisplay("display").setSystem(Constants.snomed));
                if (map.getSource().getName().equals("CIEL"))
                    values.add(new CodingDt().setCode("code").setDisplay("display").setSystem(Constants.ciel));


            }

            CodeableConceptDt codeableConceptDt = new CodeableConceptDt();
            codeableConceptDt.setCoding(values);
            observation.setValue(codeableConceptDt);

        }

        observation.setStatus(ObservationStatusEnum.FINAL);
        observation.setReliability(ObservationReliabilityEnum.OK);

        FhirContext ctx = new FhirContext();
        IParser jsonParser = ctx.newJsonParser();
        jsonParser.setPrettyPrint(true);
        String encoded = jsonParser.encodeResourceToString(observation);
        System.out.println(encoded);
        return encoded;
    }
}
