/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.validator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.ConceptClass;
import org.openmrs.OrderType;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.order.OrderUtil;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Validates the {@link OrderType} class.
 * 
 * @since 1.10
 */
@Handler(supports = { OrderType.class })
public class OrderTypeValidator implements Validator {
	
	// Log for this class
	protected final Log log = LogFactory.getLog(getClass());
	
	/**
	 * Determines if the command object being submitted is a valid type
	 * 
	 * @see org.springframework.validation.Validator#supports(java.lang.Class)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public boolean supports(Class c) {
		return OrderType.class.isAssignableFrom(c);
	}
	
	/**
	 * Validates an Order object
	 * 
	 * @see org.springframework.validation.Validator#validate(java.lang.Object,
	 *      org.springframework.validation.Errors)
	 * @should fail if the orderType object is null
	 * @should fail if name is null
	 * @should fail if name is empty
	 * @should fail if name is whitespace
	 * @should fail if name is a duplicate
	 * @should fail if conceptClass is a duplicate
	 * @should fail if parent is among its descendants
	 * @should fail if parent is also a direct child
	 * @should pass if all fields are correct for a new order type
	 * @should pass if all fields are correct for an existing order type
	 * @should be invoked when an order type is saved
	 */
	@Override
	public void validate(Object obj, Errors errors) {
		if (obj == null || !(obj instanceof OrderType)) {
			throw new IllegalArgumentException("The parameter obj should not be null and must be of type" + OrderType.class);
		} else {
			OrderType orderType = (OrderType) obj;
			String name = orderType.getName();
			if (!StringUtils.hasText(name)) {
				errors.rejectValue("name", "error.name");
				return;
			}
			
			if (orderType.getParent() != null && OrderUtil.isType(orderType, orderType.getParent())) {
				errors.rejectValue("parent", "OrderType.parent.amongDescendants", new Object[] { orderType.getName() },
				    "Parent of " + orderType.getName() + " is among its descendants");
			}
			
			OrderType duplicate = Context.getOrderService().getOrderTypeByName(name);
			if (duplicate != null && !orderType.equals(duplicate)) {
				errors.rejectValue("name", "OrderType.duplicate.name", "Duplicate order type name: " + name);
			}
			
			for (OrderType ot : Context.getOrderService().getOrderTypes(true)) {
				if (ot != null) {
					//If this was an edit, skip past the order we are actually validating 
					if (orderType.equals(ot)) {
						continue;
					}
					int index = 0;
					for (ConceptClass cc : ot.getConceptClasses()) {
						if (cc != null && orderType.getConceptClasses().contains(cc)) {
							errors.rejectValue("conceptClasses[" + index + "]", "OrderType.duplicate", new Object[] {
							        cc.getName(), orderType.getName() }, cc.getName()
							        + " is already associated to another order type:" + orderType.getName());
						}
						index++;
					}
				}
			}
		}
	}
}
