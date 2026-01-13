package com.banking.swift.model;

public enum ChargeType {
    OUR,  // All charges are borne by the ordering customer
    BEN,  // All charges are borne by the beneficiary customer
    SHA   // Charges are shared (sender's bank charges to sender, beneficiary's bank to beneficiary)
}