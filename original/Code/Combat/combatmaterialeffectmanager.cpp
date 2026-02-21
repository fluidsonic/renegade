#include "combatmaterialeffectmanager.h"
#include "materialeffect.h"
#include "transitioneffect.h"
#include "assetmgr.h"
#include "texture.h"
#include "humanstate.h"

const char * DEATH_TRANSITION_TEXTURE = "REN_death.tga";
const char * SPAWN_TRANSITION_TEXTURE = "REN_spawn.tga";
const char * HEALTH_TEXTURE = "REN_repair.tga";
const char * ELECTROCUTION_TEXTURE = "REN_shock.tga";

const float SPAWN_TRANSITION_TIME = 2.0f;

TransitionEffectClass * 
CombatMaterialEffectManager::Get_Spawn_Effect(void)
{
	TransitionEffectClass * effect = NEW_REF(TransitionEffectClass,());
	effect->Set_Parameter(1.0f);
	effect->Set_Target_Parameter(0.0f);
	effect->Set_Transition_Time(SPAWN_TRANSITION_TIME);
	effect->Enable_Remove_On_Complete(true);
	effect->Set_Max_Intensity(0.25f);

	TextureClass * tex = WW3DAssetManager::Get_Instance()->Get_Texture(SPAWN_TRANSITION_TEXTURE);
	effect->Set_Texture(tex);
	REF_PTR_RELEASE(tex);

	return effect;
}

TransitionEffectClass * 
CombatMaterialEffectManager::Get_Death_Effect(void)
{
	TransitionEffectClass * effect = NEW_REF(TransitionEffectClass,());
	effect->Set_Parameter(0.0f);
	effect->Set_Target_Parameter(1.0f);
	effect->Set_Start_Delay(0.75f * CORPSE_PERSIST_TIME);
	effect->Set_Transition_Time(0.25f * CORPSE_PERSIST_TIME);
	effect->Set_Max_Intensity(0.5f);
	effect->Set_Max_UV_Velocity(Vector2(3.75f,-6.0f));

	TextureClass * tex = WW3DAssetManager::Get_Instance()->Get_Texture(DEATH_TRANSITION_TEXTURE);
	effect->Set_Texture(tex);
	REF_PTR_RELEASE(tex);
  
	return effect;
}

TransitionEffectClass * 
CombatMaterialEffectManager::Get_Health_Effect(void)
{
	TransitionEffectClass * effect = NEW_REF(TransitionEffectClass,());
	effect->Set_Parameter(0.0f);
	effect->Set_Target_Parameter(0.49f);
	effect->Set_Transition_Time(1.0f);
	effect->Set_Max_Intensity(0.5f);

	effect->Set_Min_UV_Velocity(Vector2(0.0f,-3.0f));
	effect->Set_Max_UV_Velocity(Vector2(0.0f,-3.0f));

	TextureClass * tex = WW3DAssetManager::Get_Instance()->Get_Texture(HEALTH_TEXTURE);
	effect->Set_Texture(tex);
	REF_PTR_RELEASE(tex);

	return effect;
}

TransitionEffectClass * 
CombatMaterialEffectManager::Get_Electrocution_Effect(void)
{
	TransitionEffectClass * effect = NEW_REF(TransitionEffectClass,());
	effect->Set_Parameter(0.0f);
	effect->Set_Target_Parameter(0.49f);
	effect->Set_Transition_Time(1.0f);
	effect->Set_Max_Intensity(0.5f);

	effect->Set_Min_UV_Velocity(Vector2(0.0f,-3.0f));
	effect->Set_Max_UV_Velocity(Vector2(0.0f,-3.0f));

	TextureClass * tex = WW3DAssetManager::Get_Instance()->Get_Texture(ELECTROCUTION_TEXTURE);
	effect->Set_Texture(tex);
	REF_PTR_RELEASE(tex);

	return effect;
}

