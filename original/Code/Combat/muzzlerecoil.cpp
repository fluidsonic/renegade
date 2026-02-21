#include "muzzlerecoil.h"
#include "timemgr.h"
#include "rendobj.h"
#include "wwdebug.h"


MuzzleRecoilClass::MuzzleRecoilClass(void)
{
	Init(0);
}

void MuzzleRecoilClass::Init(int bone_index)
{
	BoneIndex = bone_index;
	RecoilScale = 1.0f;
	RecoilTimer = 0.0f;
	OORecoilTime = 0.0f;
}

void MuzzleRecoilClass::Start_Recoil(float recoil_scale,float recoil_time)
{
	RecoilScale = recoil_scale;
	RecoilTimer = recoil_time;
	if (RecoilTimer > 0.0f) {
		OORecoilTime = 1.0f / RecoilTimer;
	}
}

void MuzzleRecoilClass::Update(RenderObjClass * model)
{
	WWASSERT(model != NULL);
	if ((RecoilTimer <= 0.0f) || (BoneIndex <= 0)) {
		return;
	}

	// Since our timer is active, go ahead and capture the bone
	model->Capture_Bone(BoneIndex);

	// Apply the recoil effect.
	float recoil_scale = RecoilScale * RecoilTimer * OORecoilTime; 
	Matrix3D recoil_tm(1);
	recoil_tm.Translate_X(-recoil_scale);
	model->Control_Bone(BoneIndex,recoil_tm);

	// Decrement the recoil timer and release the bone if it expires
	RecoilTimer -= TimeManager::Get_Frame_Seconds();
	if (RecoilTimer <= 0.0f) {
		RecoilTimer = 0.0f;
		model->Release_Bone(BoneIndex);
	}
}

