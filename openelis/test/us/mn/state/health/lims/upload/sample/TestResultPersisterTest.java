/*
* The contents of this file are subject to the Mozilla Public License
* Version 1.1 (the "License"); you may not use this file except in
* compliance with the License. You may obtain a copy of the License at
* http://www.mozilla.org/MPL/ 
* 
* Software distributed under the License is distributed on an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
* License for the specific language governing rights and limitations under
* the License.
* 
* The Original Code is OpenELIS code.
* 
* Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
*/

package us.mn.state.health.lims.upload.sample;

import org.bahmni.csv.RowResult;
import org.junit.Before;
import org.mockito.Mock;
import us.mn.state.health.lims.healthcenter.dao.HealthCenterDAO;
import us.mn.state.health.lims.healthcenter.valueholder.HealthCenter;
import us.mn.state.health.lims.samplesource.dao.SampleSourceDAO;
import us.mn.state.health.lims.samplesource.valueholder.SampleSource;
import us.mn.state.health.lims.test.dao.TestDAO;
import us.mn.state.health.lims.test.valueholder.Test;
import us.mn.state.health.lims.upload.service.TestResultPersisterService;

import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class TestResultPersisterTest{

    @Mock
    private HealthCenterDAO healthCenterDAO;
    @Mock
    private TestDAO testDAO;
    @Mock
    private TestResultPersisterService testResultPersisterService;
    @Mock
    private SampleSourceDAO sampleSourceDAO;

    private TestableTestResultPersister testResultPersister;
    private String validSubscenterNameGAN;
    private List<CSVTestResult> validTestResults;
    private String validAccessionNumber;
    private String testName1;
    private String testName2;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        validSubscenterNameGAN = "GAN";
        validAccessionNumber = "123456-001";
        testName1 = "Test1";
        testName2 = "Test2";
        validTestResults = Arrays.asList(new CSVTestResult(testName1, "someValueForValue1"), new CSVTestResult(testName2, "someValueForTest2"));

        when(healthCenterDAO.getAll()).thenReturn(Arrays.asList(new HealthCenter(validSubscenterNameGAN, "Ganiyari"), new HealthCenter("SEM", "Semariya")));
        when(testDAO.getAllActiveTests(false)).thenReturn(Arrays.asList(createTest(testName1), createTest(testName2)));
        when(sampleSourceDAO.getAll()).thenReturn(Arrays.<SampleSource>asList(new SampleSource() {{this.setName("sub center");}}));

        testResultPersister = new TestableTestResultPersister(healthCenterDAO, sampleSourceDAO, testDAO, testResultPersisterService);
    }

    @org.junit.Test
    public void shouldCheckForValidSubcenterCode() throws Exception {
        CSVSamplePT10 csvSample_InvalidSubcenterCode = new CSVSamplePT10("inValidSubcenterCode", "123", validAccessionNumber, "25-02-2012", "sub center", validTestResults);
        RowResult<CSVSamplePT10> sampleRowResult = testResultPersister.validate(csvSample_InvalidSubcenterCode);

        assertFalse("Invalid subcenter code", sampleRowResult.isSuccessful());

        String[] rowWithErrorColumn = sampleRowResult.getRowWithErrorColumn();
        String erroMessage = rowWithErrorColumn[rowWithErrorColumn.length - 1];
        assertTrue("Error message incorrect", erroMessage.contains("Invalid Subcenter code"));
    }

    @org.junit.Test
    public void shouldCheckSubcenterCodeIsNotEmpty() throws Exception {
        CSVSamplePT10 csvSample_EmptySubcenterCode = new CSVSamplePT10("", "123", validAccessionNumber, "25-02-2012", "sub center", validTestResults);
        RowResult<CSVSamplePT10> sampleRowResult = testResultPersister.validate(csvSample_EmptySubcenterCode);

        assertFalse("Invalid subcenter code", sampleRowResult.isSuccessful());

        String[] rowWithErrorColumn = sampleRowResult.getRowWithErrorColumn();
        String erroMessage = rowWithErrorColumn[rowWithErrorColumn.length - 1];
        assertTrue("Error message incorrect", erroMessage.contains("Invalid Subcenter code"));
    }

    @org.junit.Test
    public void shouldCheckForCorrectnessOfTestName() throws Exception {
        List<CSVTestResult> invalidTestResults = Arrays.asList(new CSVTestResult("invalidTestName1", "valueForInvalidTest"),
                new CSVTestResult("invalidTestName2", "valueForInvalidTest"), validTestResults.get(0));
        CSVSamplePT10 csvSample_InvalidTestName = new CSVSamplePT10(validSubscenterNameGAN, "123", validAccessionNumber, "25-02-2012", "sub center", invalidTestResults);
        RowResult<CSVSamplePT10> sampleRowResult = testResultPersister.validate(csvSample_InvalidTestName);

        assertFalse("validation should fail because of invalid test name", sampleRowResult.isSuccessful());

        String[] rowWithErrorColumn = sampleRowResult.getRowWithErrorColumn();
        String erroMessage = rowWithErrorColumn[rowWithErrorColumn.length - 1];
        assertTrue("Error message incorrect", erroMessage.contains("Invalid test names: invalidTestName1,invalidTestName2"));
    }

    @org.junit.Test
    public void rowWithAtleastOneNonEmptyTestResult_ShouldBeValid() throws Exception {
        CSVTestResult emptyTestResult = new CSVTestResult("", "");
        List<CSVTestResult> testResultsWithOneEmptyTestResult = Arrays.asList(validTestResults.get(0), emptyTestResult, validTestResults.get(1));
        CSVSamplePT10 csvSample_InvalidTestName = new CSVSamplePT10(validSubscenterNameGAN, "123", validAccessionNumber, "25-02-2012", "sub center", testResultsWithOneEmptyTestResult);
        RowResult<CSVSamplePT10> sampleRowResult = testResultPersister.validate(csvSample_InvalidTestName);

        assertTrue(sampleRowResult.isSuccessful());
    }

    @org.junit.Test
    public void rowWithAllEmptyTestResult_IsOk() throws Exception {
        CSVTestResult emptyTestResult = new CSVTestResult("", "");
        List<CSVTestResult> testResultsWithAllEmptyTestResult = Arrays.asList(emptyTestResult);
        CSVSamplePT10 csvSample_InvalidTestName = new CSVSamplePT10(validSubscenterNameGAN, "123", validAccessionNumber, "25-02-2012", "sub center", testResultsWithAllEmptyTestResult);
        RowResult<CSVSamplePT10> sampleRowResult = testResultPersister.validate(csvSample_InvalidTestName);

        assertTrue("empty test result rows are ok", sampleRowResult.isSuccessful());
    }

    @org.junit.Test
    public void testNeedNotHaveResults() throws Exception {
        CSVTestResult invalidTestResult = new CSVTestResult(validTestResults.get(0).test, "");
        List<CSVTestResult> testResultsWithOneInvalidTestResult = Arrays.asList(invalidTestResult, validTestResults.get(1));
        CSVSamplePT10 csvSample_InvalidTestName = new CSVSamplePT10(validSubscenterNameGAN, "123", validAccessionNumber, "25-02-2012", "sub center", testResultsWithOneInvalidTestResult);
        RowResult<CSVSamplePT10> sampleRowResult = testResultPersister.validate(csvSample_InvalidTestName);

        assertTrue("tests need not have results", sampleRowResult.isSuccessful());
    }

    @org.junit.Test
    public void accessionNumberShouldNotBeBlank() throws Exception {
        CSVSamplePT10 csvSample_blankAccessionNumber = new CSVSamplePT10(validSubscenterNameGAN, "123", "", "25-02-2012", "sub center", validTestResults);
        RowResult<CSVSamplePT10> sampleRowResult = testResultPersister.validate(csvSample_blankAccessionNumber);

        assertFalse("validation should fail because of blank accession number", sampleRowResult.isSuccessful());

        String[] rowWithErrorColumn = sampleRowResult.getRowWithErrorColumn();
        String erroMessage = rowWithErrorColumn[rowWithErrorColumn.length - 1];
        assertTrue("Error message incorrect", erroMessage.contains("AccessionNumber should not be blank"));
    }

    @org.junit.Test
    public void accessionNumberShouldBeInAValidFormat() throws Exception {
        CSVSamplePT10 csvSample_blankAccessionNumber = new CSVSamplePT10(validSubscenterNameGAN, "123", "invalidAccessionNumber", "25-02-2012", "sub center", validTestResults);
        RowResult<CSVSamplePT10> sampleRowResult = testResultPersister.validate(csvSample_blankAccessionNumber);

        assertFalse("validation should fail because of invalid accession number", sampleRowResult.isSuccessful());

        String[] rowWithErrorColumn = sampleRowResult.getRowWithErrorColumn();
        String erroMessage = rowWithErrorColumn[rowWithErrorColumn.length - 1];
        assertTrue("Error message incorrect", erroMessage.contains("AccessionNumber format is invalid"));
    }

    @org.junit.Test
    public void sampleDateShouldBeOfValidFormatOf_dd_mm_yyyy() throws Exception {
        CSVSamplePT10 csvSample_invalidDate = new CSVSamplePT10(validSubscenterNameGAN, "123", "invalidAccessionNumber", "02-25-2012", "sub center", validTestResults);
        RowResult<CSVSamplePT10> sampleRowResult = testResultPersister.validate(csvSample_invalidDate);

        assertFalse("validation should fail because of invalid date format", sampleRowResult.isSuccessful());

        String[] rowWithErrorColumn = sampleRowResult.getRowWithErrorColumn();
        String erroMessage = rowWithErrorColumn[rowWithErrorColumn.length - 1];
        assertTrue("Error message incorrect", erroMessage.contains("Date should be in dd-mm-yyyy format"));
    }

    @org.junit.Test
    public void shouldBeValid() throws Exception {
        CSVSamplePT10 validCsvSample = new CSVSamplePT10(validSubscenterNameGAN, "123", validAccessionNumber, "25-02-2012", "sub center", validTestResults);
        RowResult<CSVSamplePT10> sampleRowResult = testResultPersister.validate(validCsvSample);

        assertTrue("validation should pass", sampleRowResult.isSuccessful());
    }

    @org.junit.Test
    public void shouldPersistTestResults() {
        CSVSamplePT10 validCsvSample = new CSVSamplePT10(validSubscenterNameGAN, "123", validAccessionNumber, "25-02-2012", "sub center", validTestResults);
        testResultPersister.persist(validCsvSample);
        verify(testResultPersisterService).persist(validCsvSample);
    }

    @org.junit.Test
    public void shouldBeInvalid_when_sampleSourceIsBlank() {
        CSVSamplePT10 invalidCsvSample = new CSVSamplePT10(validSubscenterNameGAN, "123", validAccessionNumber, "25-02-2012", "", validTestResults);
        RowResult<CSVSamplePT10> rowResult = testResultPersister.validate(invalidCsvSample);

        assertFalse(rowResult.isSuccessful());
        String[] rowWithErrorColumn = rowResult.getRowWithErrorColumn();
        String errorMessage = rowWithErrorColumn[rowWithErrorColumn.length - 1];
        assertTrue(errorMessage.contains("Invalid Sample source.\n"));
    }

    @org.junit.Test
    public void shouldBeInvalid_when_sampleSourceIsNotFromDB() {
        CSVSamplePT10 invalidCsvSample = new CSVSamplePT10(validSubscenterNameGAN, "123", validAccessionNumber, "25-02-2012", "not from db", validTestResults);
        RowResult<CSVSamplePT10> rowResult = testResultPersister.validate(invalidCsvSample);

        assertFalse(rowResult.isSuccessful());
        String[] rowWithErrorColumn = rowResult.getRowWithErrorColumn();
        String errorMessage = rowWithErrorColumn[rowWithErrorColumn.length - 1];
        assertTrue(errorMessage.contains("Invalid Sample source.\n"));
    }

    public class TestableTestResultPersister extends TestResultPersister{

        private String stNumberFormat = "/([a-zA-Z]*)(\\d+)/";

        public TestableTestResultPersister(HealthCenterDAO healthCenterDAO, SampleSourceDAO sampleSourceDAO, TestDAO testDAO, TestResultPersisterService testResultPersisterService) {
            super(healthCenterDAO, sampleSourceDAO, testDAO, testResultPersisterService);
        }

        @Override
        protected String getStNumberFormat() {
            return stNumberFormat;
        }
    }

    private Test createTest(final String testName) {
        return new Test() {{
            this.setTestName(testName);
        }};
    }
}
