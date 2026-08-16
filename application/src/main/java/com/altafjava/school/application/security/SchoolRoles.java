package com.altafjava.school.application.security;

import com.altafjava.platform.core.security.Roles;

// Role names/SpEL fragments for TEACHER/PARENT/STUDENT — mirrors platform's Roles class.
public final class SchoolRoles {

	public static final String TEACHER = "TEACHER";
	public static final String PARENT = "PARENT";
	public static final String STUDENT = "STUDENT";

	public static final String HAS_TENANT_ADMIN_OR_TEACHER = "hasAnyRole('" + Roles.TENANT_ADMIN + "', '" + TEACHER
			+ "')";
	public static final String HAS_TENANT_ADMIN_OR_TEACHER_OR_PARENT_OR_STUDENT = "hasAnyRole('" + Roles.TENANT_ADMIN
			+ "', '" + TEACHER + "', '" + PARENT + "', '" + STUDENT + "')";
	public static final String HAS_TENANT_ADMIN_OR_PARENT_OR_STUDENT = "hasAnyRole('" + Roles.TENANT_ADMIN + "', '"
			+ PARENT + "', '" + STUDENT + "')";
	public static final String HAS_PARENT = "hasRole('" + PARENT + "')";

	private SchoolRoles() {
	}
}
