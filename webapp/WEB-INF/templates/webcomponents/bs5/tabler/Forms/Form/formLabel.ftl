<#-- Macro: formLabel

Description: Generates a label for a Bootstrap form group.

Parameters:
- class (string, optional): additional CSS classes to add to the label element.
- labelFor (string, optional): the ID of the input field that the label is associated with.
- labelId (string, optional): the ID of the label element.
- labelKey (string, optional): the internationalization key for the label text.
- hideLabel (list, optional): a list of label properties to hide (e.g. "label", "i18nLabel").
- mandatory (boolean, optional): whether the input field is mandatory.
- deprecated: whether the macro is deprecated.

-->
<#macro formLabel class='' labelFor='' labelId='' labelKey='' hideLabel=[] mandatory=true deprecated...>
<@deprecatedWarning args=deprecated />	
<#local labelClass = ' ' + displaySettings(hideLabel,'') />
<label class="form-label<#if mandatory=true> required</#if><#if class !=''> ${class?trim}</#if><#if hideLabel?seq_contains('all')> sr-only</#if> " for="${labelFor}" <#if labelId!=''> id="${labelId}"</#if><#if mandatory=true>ariaLabel="${labelKey} [#i18n{portal.users.modify_attribute.labelMandatory}]"</#if>>
	<#if labelKey !=''><span class="${labelClass}">${labelKey} </span><#else><#nested></#if>
</label>
</#macro>