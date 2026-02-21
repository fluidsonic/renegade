
#ifndef PROGCALL_H
#define PROGCALL_H

/*
** This is the base class definition for a progress callback to be used with some of the 
** more expensive functions in the physics system.  Derive your own class and override the
** methods you want to intercept (probably just Notify_Completed).
*/
class ProgressCallbackClass
{
public:

	ProgressCallbackClass(void);
	virtual ~ProgressCallbackClass(void);

	virtual void	Reset(void);
	virtual void	Add_Cost(int amount);
	virtual void	Notify_Completed(int amount);
	float				Percent_Done(void);

protected:

	int				TotalCost;
	int				Completed;
};



#endif

