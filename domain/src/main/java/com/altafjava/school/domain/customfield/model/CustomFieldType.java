package com.altafjava.school.domain.customfield.model;

// The primitive types a tenant-defined custom field value may hold. CustomFieldValueService
// validates every stored value's textual representation against the owning definition's type
// before it ever reaches platform's EntityAttributeService (which stores plain strings only).
public enum CustomFieldType {
	TEXT, NUMBER, DATE, BOOLEAN
}
