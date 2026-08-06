package OOESEMHierarcyExample {
	private import OOSEM::OOSEM_Metadata::*;
	
	#specification part def S_v1 {
		#specification s_par : SubSpec;
	}
	
	#specification part def S_v2 :> S_v1 {
		
	}
	
	#design part def D_v1 :> S_v2 {
		#design part d_par :>> s_par : SubDes;
	}
	
	#design part def D_v2 :> D_v1 {
		
	}
	
	#integration part def I_v1 :> D_v2 {
		#design i_par :>> d_par : SubInt;
	}
	
	#integration part def I_v2 :> I_v1 {
		
	}
	
	#specification part def SubSpec;
	#design part def SubDes :> SubSpec;
	#integration part def SubInt :> SubDes;
}