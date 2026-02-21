#ifndef REFPTR_H
#define REFPTR_H

#include "VisualC.h"
#include "RefCounted.h"
#include <stddef.h>
#include <assert.h>

template<typename Type> class RefPtr;
template<typename Type> class RefPtrConst;

class RefPtrBase
	{
	public:
		inline bool operator==(const RefPtrBase& rhs) const
			{return (mRefObject == rhs.mRefObject);}

		inline bool operator!=(const RefPtrBase& rhs) const
			{return !operator==(rhs);}

		inline bool IsValid(void) const
			{return (mRefObject != NULL);}

		inline void Detach(void)
			{
			if (IsValid())
				{
				mRefObject->Release();
				mRefObject = NULL;
				}
			}

	protected:
		RefPtrBase()
			: mRefObject(NULL)
			{}

		RefPtrBase(RefCounted* object)
			: mRefObject(object)
			{
			assert((mRefObject == NULL) || (mRefObject->mRefCount == 0));
		
			if (IsValid())
				{
				mRefObject->AddReference();
				}
			}

		RefPtrBase(const RefPtrBase& object)
			: mRefObject(object.mRefObject)
			{
			assert(false); // why is this being called?
	
			if (IsValid())
				{
				mRefObject->AddReference();
				}
			}

		virtual ~RefPtrBase()
			{Detach();}

		const RefPtrBase& operator=(const RefPtrBase&);
	
		inline RefCounted* const GetRefObject(void)
			{return mRefObject;}

		inline const RefCounted* const GetRefObject(void) const
			{return mRefObject;}

		inline void Attach(RefCounted* object)
			{
			// If objects are different
			if (object != mRefObject)
				{
				// Add reference to new object
				if (object != NULL)
					{
					object->AddReference();
					}

				// Release reference to old object
				Detach();

				// Assign new object
				mRefObject = object;
				}
			}

	private:
		RefCounted* mRefObject;

		template<typename Derived>
		friend RefPtr<Derived> Dynamic_Cast(RefPtrBase&);

		template<typename Type>
		friend RefPtr<Type> Reinterpret_Cast(RefPtrBase&);
	};


template<typename Type> class RefPtr
	: public RefPtrBase
	{
	public:
		RefPtr()
			: RefPtrBase()
			{}

		template<typename Derived>
		RefPtr(const RefPtr<Derived>& derived)
			: RefPtrBase()
			{
			Attach(const_cast<Derived*>(derived.ReferencedObject()));
			}

		RefPtr(const RefPtr<Type>& object)
			: RefPtrBase()
			{
			Attach(const_cast<Type*>(object.ReferencedObject()));
			}

		virtual ~RefPtr()
			{}

		template<typename Derived>
		inline const RefPtr<Type>& operator=(const RefPtr<Derived>& derived)
			{
			Attach(const_cast<Derived*>(derived.ReferencedObject()));
			return *this;
			}

		inline const RefPtr<Type>& operator=(const RefPtr<Type>& object)
			{
			Attach(const_cast<Type*>(object.ReferencedObject()));
			return *this;
			}

		inline Type& operator*() const
			{
			assert(IsValid());
			return *const_cast<Type*>(ReferencedObject());
			}

		inline Type* const operator->() const
			{
			assert(IsValid());
			return const_cast<Type*>(ReferencedObject());
			}

		// These are public mostly because I can't seem to declare rc_ptr<Other> as a friend
		inline Type* const ReferencedObject(void)
			{return reinterpret_cast<Type*>(GetRefObject());}

		inline const Type* const ReferencedObject(void) const
			{return reinterpret_cast<const Type*>(GetRefObject());}

		RefPtr(Type* object)
			: RefPtrBase()
			{
			Attach(object);
			}

		inline const RefPtr<Type>& operator=(Type* object)
			{
			Attach(object);
			return *this;
			}

	private:
		friend RefPtr<Type> Dynamic_Cast(RefPtrBase&);
		friend RefPtr<Type> Reinterpret_Cast(RefPtrBase&);
		friend RefPtr<Type> Const_Cast(RefPtrConst<Type>&);
	};


template<typename Type> class RefPtrConst
	: public RefPtrBase
	{
	public:
		RefPtrConst()
			: RefPtrConst()
			{}

		template<typename Derived>
		RefPtrConst(const RefPtr<Derived>& derived)
			: RefPtrBase()
			{
			Attach(derived.ReferencedObject());
			}

		RefPtrConst(const RefPtr<Type>& object)
			: RefPtrBase()
			{
			Attach(const_cast<Type* const >(object.ReferencedObject()));
			}

		template<typename Derived>
		RefPtrConst(const RefPtrConst<Derived>& derived)
			: RefPtrBase()
			{
			Attach(derived.ReferencedObject());
			}

		RefPtrConst(const RefPtrConst<Type>& object)
			: RefPtrBase()
			{
			Attach(object.ReferencedObject());
			}

		template<typename Derived>
		inline const RefPtrConst<Type>& operator=(const RefPtr<Derived>& derived)
			{
			Attach(derived.ReferencedObject());
			return *this;
			}

		inline const RefPtrConst<Type>& operator=(const RefPtr<Type>& object)
			{
			Attach(object.ReferencedObject());
			return *this;
			}

		template<typename Derived>
		inline const RefPtrConst<Type>& operator=(const RefPtrConst<Derived>& derived)
			{
			Attach(derived.ReferencedObject());
			return *this;
			}

		inline const RefPtrConst<Type>& operator=(const RefPtrConst<Type>& object)
			{
			Attach(object.ReferencedObject());
			return *this;
			}

		virtual ~RefPtrConst()
			{}

		inline const Type& operator*() const
			{
			assert(IsValid());
			return *ReferencedObject();
			}

		inline const Type* const operator->() const
			{
			assert(IsValid());
			return ReferencedObject();
			}

		// This is public mostly because I can't seem to declare rc_ptr<Other> as a friend
		inline const Type* const ReferencedObject() const
			{return reinterpret_cast<const Type*>(GetRefObject());}

		RefPtrConst(const Type* object)
			: RefPtrBase()
			{
			Attach(object);
			}

		const RefPtrConst<Type>& operator=(const Type* object)
			{
			Attach(object);
			}
	};


template<typename Derived>
RefPtr<Derived> Dynamic_Cast(RefPtrBase& base)
	{
	RefPtr<Derived> derived;
	derived.Attach(base.GetRefObject());
	return derived;
	}


template<typename Type>
RefPtr<Type> Reinterpret_Cast(RefPtrBase& rhs)
	{
	RefPtr<Type> object;
	object.Attach(rhs.GetRefObject());
	return object;
	}


template<typename Type>
RefPtr<Type> Const_Cast(RefPtrConst<Type>& rhs)
	{
	RefPtr<Type> object;
	object.Attach(rhs.ReferencedObject());
	return object;
	}

#endif // RC_PTR_H
