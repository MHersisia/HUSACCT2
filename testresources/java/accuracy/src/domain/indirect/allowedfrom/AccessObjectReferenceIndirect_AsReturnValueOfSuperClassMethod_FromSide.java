package domain.indirect.violatingfrom;

import domain.indirect.BaseIndirect;

public class AccessObjectReferenceIndirect_AsReturnValueOfSuperClassMethod_FromSide extends BaseIndirect {
	
	public String initializeProfileInformation(){
		Object o = getProfileInformation("tsja", 3);
		return o;
	}
}