#if 0 // TODO finish this some day...

class CloudShadowClass : public VertexProcessorClass
{
public:

	CloudShadowClass(void);
	~CloudShadowClass(void)	{}
	
	// scene graph? transform into eye space
	void						Set_Camera(const CameraClass & cam);
	
	// vertex processor interface
	virtual SRBOOL			isActive(srVertexPipe&);
	virtual void			process(srVertexPipe&);

	// fog volume interface
	float						UOffset;
	float						VOffset;
	Vector3					Center;
	Vector3					Scale;
	
	srMatrix4				modelview;

protected:
	
	srColorSurfaceIFace *ShadowTexture;

};


CloudShadowClass::CloudShadowClass(void) :
	ShadowTexture(NULL),
	TextureWidth(0),
	TextureHeight(0),
	UOffset(0.0f),
	VOffset(0.0f),
	Center(0,0,0),
	Scale(75.0f,75.0f,1.0f)
{
	ShadowTexture = srCore.getSurfaceIOManager()->importSurface( "clouds.tga" );
	TextureWidth = ShadowTexture->getWidth();
	TextureHeight = ShadowTexture->getHeight();
}

CloudShadowClass::~CloudShadowClass(void)	
{
	ShadowTexture->release();
}

void CloudShadowClass::Process_Push(srGERD * gerd)
{
	// CloudShadows are applied in world space, 
	srMatrix4 modelview;
	gerd.matrixMode(srGERD::MODELVIEW);
	gerd->getMatrix(modelView);
	modelview.invert();
	
	
	greg->pushVertexProcessor(*this);
}

void CloudShadowClass::Process_Pop(srGERD * gerd)
{
	gerd->popVertexProcessor();
}

SRBOOL CloudShadowClass::isActive(srVertexPipe&)
{
	return pipe.isChannelAvailable(CHANNEL_DIFFUSE);
}

void CloudShadowClass::process(srVertexPipe&)
{
	// screen-mapped uv coordinates
	if (pipe.isChannelAvailable (CHANNEL_DIFFUSE))			
	{
		int vnum = pipe.getVertexCount();
		srVector4 * loc = pipe.getEyeSpaceLocation(void);
		srVector4 * diffuse = pipe.getDiffuse(void);
	
		for (int vidx = 0; vidx < vnum; vidx++) {	
			
			SRLONG x = (loc[vidx].x - Center.X) * TextureWidth / Scale.X;
			SRLONG y = (loc[vidx].y - Center.Y) * TextureHeight / Scale.Y;
			srARGB pixel = ShadowTexture->getPixel(x,y); 			
			diffuse[vidx].r = pixel.getRed();
			diffuse[vidx].g = pixel.getGreen();
			diffuse[vidx].b = pixel.getBlue();

		}
	}
}

#endif