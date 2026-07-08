# File: create-jmeter-test-plan.ps1

param(
    [string]$OutputPath = "C:\Users\ducho\Desktop\fraudlens-api\jmeter-tests\fraudlens-load-test.jmx"
)

# Stelle sicher dass Directory existiert
$Dir = Split-Path -Parent $OutputPath
if (!(Test-Path $Dir)) {
    New-Item -ItemType Directory -Path $Dir -Force | Out-Null
}

# JMeter Test Plan XML
$TestPlanXML = @'
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="FraudLens Load Test" enabled="true">
      <elementProp name="TestPlan.user_defined_variables" elementType="Arguments" guiclass="ArgumentsPanel" testclass="Arguments" testname="User Defined Variables" enabled="true">
        <collectionProp name="Arguments.arguments"/>
      </elementProp>
      <stringProp name="TestPlan.user_define_classpath"></stringProp>
      <booleanProp name="TestPlan.serialize_threadgroups">false</booleanProp>
      <booleanProp name="TestPlan.functional_mode">false</booleanProp>
      <booleanProp name="TestPlan.tearDown_on_shutdown">true</booleanProp>
      <booleanProp name="TestPlan.save_thread_information">true</booleanProp>
      <booleanProp name="TestPlan.delayedStart">false</booleanProp>
      <booleanProp name="TestPlan.includeControllers">true</booleanProp>
    </TestPlan>
    <hashTree>
      <!-- Thread Group: Health Check -->
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="Health Check" enabled="true">
        <elementProp name="ThreadGroup.main_controller" elementType="LoopController" guiclass="LoopControlPanel" testclass="LoopController" testname="Loop Controller" enabled="true">
          <booleanProp name="LoopController.continue_forever">false</booleanProp>
          <stringProp name="LoopController.loops">5</stringProp>
        </elementProp>
        <stringProp name="ThreadGroup.num_threads">5</stringProp>
        <stringProp name="ThreadGroup.ramp_time">10</stringProp>
        <elementProp name="ThreadGroup.scheduler" elementType="SchedulerTime" guiclass="SchedulerTimePanel" testclass="SchedulerTime" testname="Scheduler" enabled="false">
          <booleanProp name="SchedulerTime.sliced">false</booleanProp>
          <booleanProp name="SchedulerTime.delay">false</booleanProp>
          <booleanProp name="SchedulerTime.duration">false</booleanProp>
          <stringProp name="SchedulerTime.delay_length"></stringProp>
          <stringProp name="SchedulerTime.duration_length"></stringProp>
        </elementProp>
      </ThreadGroup>
      <hashTree>
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="GET Health Check" enabled="true">
          <elementProp name="HTTPsampler.Arguments" elementType="Arguments" guiclass="HTTPArgumentsPanel" testclass="Arguments" testname="User Defined Variables" enabled="true">
            <collectionProp name="Arguments.arguments"/>
          </elementProp>
          <stringProp name="HTTPSampler.domain">localhost</stringProp>
          <stringProp name="HTTPSampler.port">8080</stringProp>
          <stringProp name="HTTPSampler.protocol">http</stringProp>
          <stringProp name="HTTPSampler.contentEncoding"></stringProp>
          <stringProp name="HTTPSampler.path">/api/v1/invoices/health</stringProp>
          <stringProp name="HTTPSampler.method">GET</stringProp>
          <booleanProp name="HTTPSampler.follow_redirects">true</booleanProp>
          <booleanProp name="HTTPSampler.auto_redirects">false</booleanProp>
          <booleanProp name="HTTPSampler.use_keepalive">true</booleanProp>
          <booleanProp name="HTTPSampler.DO_MULTIPART_POST">false</booleanProp>
          <stringProp name="HTTPSampler.embedded_url_re"></stringProp>
          <stringProp name="HTTPSampler.connect_timeout"></stringProp>
          <stringProp name="HTTPSampler.response_timeout"></stringProp>
        </HTTPSamplerProxy>
        <hashTree>
          <ResponseAssertion guiclass="AssertionGui" testclass="ResponseAssertion" testname="Assert Response OK" enabled="true">
            <elementProp name="TestElements" elementType="CollectionProperty" guiclass="CollectionProperty" testclass="CollectionProperty" testname="Assertions" enabled="true">
              <stringProp name="-1">Assertion.response_data</stringProp>
              <stringProp name="1">healthy</stringProp>
            </elementProp>
            <stringProp name="Assertion.test_type">6</stringProp>
            <booleanProp name="Assertion.assume_success">false</booleanProp>
            <intProp name="Assertion.test_fields">2</intProp>
          </ResponseAssertion>
          <hashTree/>
        </hashTree>
      </hashTree>
      <!-- JSON Analysis Thread Group -->
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="JSON Analysis" enabled="true">
        <elementProp name="ThreadGroup.main_controller" elementType="LoopController" guiclass="LoopControlPanel" testclass="LoopController" testname="Loop Controller" enabled="true">
          <booleanProp name="LoopController.continue_forever">false</booleanProp>
          <stringProp name="LoopController.loops">3</stringProp>
        </elementProp>
        <stringProp name="ThreadGroup.num_threads">10</stringProp>
        <stringProp name="ThreadGroup.ramp_time">30</stringProp>
      </ThreadGroup>
      <hashTree>
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="POST JSON Analysis" enabled="true">
          <elementProp name="HTTPsampler.Arguments" elementType="Arguments" guiclass="HTTPArgumentsPanel" testclass="Arguments" testname="User Defined Variables" enabled="true">
            <collectionProp name="Arguments.arguments"/>
          </elementProp>
          <stringProp name="HTTPSampler.domain">localhost</stringProp>
          <stringProp name="HTTPSampler.port">8080</stringProp>
          <stringProp name="HTTPSampler.protocol">http</stringProp>
          <stringProp name="HTTPSampler.contentEncoding">utf-8</stringProp>
          <stringProp name="HTTPSampler.path">/api/v1/invoices/analyze/json</stringProp>
          <stringProp name="HTTPSampler.method">POST</stringProp>
          <booleanProp name="HTTPSampler.follow_redirects">true</booleanProp>
          <booleanProp name="HTTPSampler.auto_redirects">false</booleanProp>
          <booleanProp name="HTTPSampler.use_keepalive">true</booleanProp>
          <booleanProp name="HTTPSampler.DO_MULTIPART_POST">false</booleanProp>
          <stringProp name="HTTPSampler.embedded_url_re"></stringProp>
          <stringProp name="HTTPSampler.connect_timeout"></stringProp>
          <stringProp name="HTTPSampler.response_timeout"></stringProp>
          <booleanProp name="HTTPsampler.postBodyRaw">true</booleanProp>
          <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
            <stringProp name="Argument.value">{
  "invoiceNumber": "RE-TEST-${__time()}",
  "workshopName": "Test Workshop",
  "grossAmount": 1500.00,
  "licensePlate": "M-AB 1234",
  "netAmount": 1261.00,
  "vatAmount": 239.00,
  "taxNumber": "123/456/78901"
}</stringProp>
          </elementProp>
        </HTTPSamplerProxy>
        <hashTree>
          <HeaderManager guiclass="HeaderPanel" testclass="HeaderManager" testname="HTTP Header Manager" enabled="true">
            <collectionProp name="HeaderManager.headers">
              <elementProp name="Content-Type" elementType="Header">
                <stringProp name="Header.name">Content-Type</stringProp>
                <stringProp name="Header.value">application/json</stringProp>
              </elementProp>
            </collectionProp>
          </HeaderManager>
          <hashTree/>
          <ResponseAssertion guiclass="AssertionGui" testclass="ResponseAssertion" testname="Assert Response Has riskScore" enabled="true">
            <elementProp name="TestElements" elementType="CollectionProperty" guiclass="CollectionProperty" testclass="CollectionProperty" testname="Assertions" enabled="true">
              <stringProp name="-1">Assertion.response_data</stringProp>
              <stringProp name="1">riskScore</stringProp>
            </elementProp>
            <stringProp name="Assertion.test_type">6</stringProp>
            <booleanProp name="Assertion.assume_success">false</booleanProp>
            <intProp name="Assertion.test_fields">2</intProp>
          </ResponseAssertion>
          <hashTree/>
        </hashTree>
      </hashTree>
      <!-- Summary Report Listener -->
      <ResultCollector guiclass="StatVisualizer" testclass="ResultCollector" testname="Summary Report" enabled="true">
        <elementProp name="collectorProperties" elementType="CollectionProperty" guiclass="CollectionProperty" testclass="CollectionProperty"/>
        <stringProp name="filename"></stringProp>
        <booleanProp name="ResultCollector.error_logging">false</booleanProp>
        <objProp>
          <n>value</n>
          <value class="SampleSaveConfiguration">
            <time>true</time>
            <latency>true</latency>
            <timestamp>true</timestamp>
            <success>true</success>
            <label>true</label>
            <code>true</code>
            <message>true</message>
            <threadName>true</threadName>
            <dataType>true</dataType>
            <encoding>false</encoding>
            <assertions>true</assertions>
            <subresults>true</subresults>
            <responseData>false</responseData>
            <samplerData>false</samplerData>
            <xml>true</xml>
            <fieldNames>true</fieldNames>
            <responseHeaders>false</responseHeaders>
            <requestHeaders>false</requestHeaders>
            <responseDataOnError>false</responseDataOnError>
            <saveAssertionResultsFailureMessage>true</saveAssertionResultsFailureMessage>
            <assertionsResultsToSave>0</assertionsResultsToSave>
            <bytes>true</bytes>
            <sentBytes>true</sentBytes>
            <url>true</url>
            <filename>true</filename>
            <hostname>true</hostname>
            <threadCounts>true</threadCounts>
            <sampleCount>true</sampleCount>
            <idleTime>true</idleTime>
            <connectTime>true</connectTime>
          </value>
        </objProp>
      </ResultCollector>
      <hashTree/>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
'@

# Speichere XML in Datei
Set-Content -Path $OutputPath -Value $TestPlanXML -Encoding UTF8

Write-Host "✅ Test plan created successfully!" -ForegroundColor Green
Write-Host "Location: $OutputPath" -ForegroundColor Yellow