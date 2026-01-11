package com.banking.sepa.service;

import com.banking.sepa.model.SepaTransfer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class IsoXmlGeneratorService {

    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    public String generatePaymentInitiationXml(SepaTransfer transfer) {
        log.info("Generating ISO 20022 XML for SEPA transfer: {}", transfer.getSepaReference());

        StringBuilder xml = new StringBuilder();
        xml.append(XML_HEADER).append("\n");

        switch (transfer.getTransferType()) {
            case SCT:
            case SCT_INST:
                xml.append(generateCreditTransferXml(transfer));
                break;
            case SDD_CORE:
            case SDD_B2B:
                xml.append(generateDirectDebitXml(transfer));
                break;
        }

        String isoXml = xml.toString();
        log.debug("Generated ISO 20022 XML: {} characters", isoXml.length());

        return isoXml;
    }

    private String generateCreditTransferXml(SepaTransfer transfer) {
        // ISO 20022 pain.001.001.03 (Customer Credit Transfer Initiation)
        StringBuilder xml = new StringBuilder();

        xml.append("<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pain.001.001.03\">\n");
        xml.append("  <CstmrCdtTrfInitn>\n");

        // Group Header
        xml.append("    <GrpHdr>\n");
        xml.append("      <MsgId>").append(transfer.getMessageId()).append("</MsgId>\n");
        xml.append("      <CreDtTm>").append(transfer.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME))
           .append("</CreDtTm>\n");
        xml.append("      <NbOfTxs>1</NbOfTxs>\n");
        xml.append("      <CtrlSum>").append(transfer.getAmount()).append("</CtrlSum>\n");
        xml.append("    </GrpHdr>\n");

        // Payment Information
        xml.append("    <PmtInf>\n");
        xml.append("      <PmtInfId>").append(transfer.getSepaReference()).append("</PmtInfId>\n");
        xml.append("      <PmtMtd>TRF</PmtMtd>\n");

        if (transfer.getTransferType() == SepaTransfer.SepaTransferType.SCT_INST) {
            xml.append("      <SvcLvl><Cd>SEPA</Cd></SvcLvl>\n");
            xml.append("      <LclInstrm><Cd>INST</Cd></LclInstrm>\n");
        }

        xml.append("      <ReqdExctnDt>")
           .append(transfer.getRequestedExecutionDate() != null ?
                   transfer.getRequestedExecutionDate().toString() :
                   transfer.getCreatedAt().toLocalDate().toString())
           .append("</ReqdExctnDt>\n");

        // Debtor
        xml.append("      <Dbtr>\n");
        xml.append("        <Nm>").append(escapeXml(transfer.getDebtorName())).append("</Nm>\n");
        xml.append("      </Dbtr>\n");

        xml.append("      <DbtrAcct>\n");
        xml.append("        <Id><IBAN>").append(transfer.getDebtorIban()).append("</IBAN></Id>\n");
        xml.append("      </DbtrAcct>\n");

        // Credit Transfer Transaction
        xml.append("      <CdtTrfTxInf>\n");
        xml.append("        <PmtId>\n");
        xml.append("          <EndToEndId>").append(transfer.getEndToEndId()).append("</EndToEndId>\n");
        xml.append("        </PmtId>\n");

        xml.append("        <Amt>\n");
        xml.append("          <InstdAmt Ccy=\"").append(transfer.getCurrency()).append("\">")
           .append(transfer.getAmount()).append("</InstdAmt>\n");
        xml.append("        </Amt>\n");

        // Creditor
        xml.append("        <Cdtr>\n");
        xml.append("          <Nm>").append(escapeXml(transfer.getCreditorName())).append("</Nm>\n");
        xml.append("        </Cdtr>\n");

        xml.append("        <CdtrAcct>\n");
        xml.append("          <Id><IBAN>").append(transfer.getCreditorIban()).append("</IBAN></Id>\n");
        xml.append("        </CdtrAcct>\n");

        // Remittance Information
        if (transfer.getRemittanceInformation() != null) {
            xml.append("        <RmtInf>\n");
            xml.append("          <Ustrd>").append(escapeXml(transfer.getRemittanceInformation()))
               .append("</Ustrd>\n");
            xml.append("        </RmtInf>\n");
        }

        xml.append("      </CdtTrfTxInf>\n");
        xml.append("    </PmtInf>\n");
        xml.append("  </CstmrCdtTrfInitn>\n");
        xml.append("</Document>");

        return xml.toString();
    }

    private String generateDirectDebitXml(SepaTransfer transfer) {
        // ISO 20022 pain.008.001.02 (Customer Direct Debit Initiation)
        // Simplified version
        return "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pain.008.001.02\">" +
               "<CstmrDrctDbtInitn><!-- Direct Debit XML structure --></CstmrDrctDbtInitn>" +
               "</Document>";
    }

    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}
