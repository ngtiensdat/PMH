-- Migration: Add IP_ADDRESS column to PMH_AUDIT_LOG
ALTER TABLE PMH_AUDIT_LOG ADD (IP_ADDRESS VARCHAR2(100 CHAR));

-- Fix: Drop redundant ID column from PMH_COMPONENTS (primary key is COMPONENT_CODE)
ALTER TABLE PMH_COMPONENTS DROP COLUMN ID;

-- Fix: Drop FK constraint to allow storing comma-separated multi-select component codes
ALTER TABLE PMH_GROUP_CATEGORY DROP CONSTRAINT FK_CATEGORY_COMPONENT;
