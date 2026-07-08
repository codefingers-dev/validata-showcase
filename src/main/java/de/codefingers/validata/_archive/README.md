# Archived Code - Deprecated Claude Integration

## Warum archiviert?
- Claude ist overkill für regelbasierte Probleme
- Pure Rules-Based Approach ist besser (80-90% Coverage)
- 10x schneller, 100x billiger
- Bessere Transparenz für deutsche Versicherer

## Archivierte Klassen
- BedrockAnalysisService.java (AWS Bedrock / Claude)
- PromptBuilder.java (Prompt Orchestration)
- SystemPromptTemplate.java (System Instructions)
- FraudAnalysisExamples.java (Few-Shot Examples)
+ alle entsprechenden Test-Klassen

## Wenn Claude später brauchst
git log --follow -- src/main/java/de/codefingers/fraudlens/_archive/deprecated/bedrock/BedrockAnalysisService.java

Version: v2.0 (Pure Rules-Based Detection)
