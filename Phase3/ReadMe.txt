1. Make sure that phase3.jar and StopWords.txt are in the same directory.
2. In the command line prompt, navigate to the above directory.
3. Type in (without the quotes): "java -jar phase3.jar param1 param2 param3 param4", where param1, param2, param3 and param4 are custom parameters you choose to decide what task, and how, to run. Not all 4 parameters are necessary since it depends specifically on the task. The following list will show the possible options to input.


param1		param2		param3		param4		param5

TASK1		KV
TASK1		LDA
TASK1		SVD
TASK1		PCA

TASK2		TF
TASK2		TF-IDF

TASK3		k

TASK4		k			AUTHOR		KV
TASK4		k			AUTHOR		LDA
TASK4		k			AUTHOR		SVD
TASK4		k			AUTHOR		PCA
TASK4		k			PAPER		TF
TASK4		k			PAPER		TF-IDF

TASK5		k			authorID	KV
TASK5		k			authorID	LDA
TASK5		k			authorID	SVD
TASK5		k			authorID	PCA
TASK5		k			paperID		TF
TASK5		k			paperID		TF-IDF

TASK6		k			AUTHOR		KV			authorID
TASK6		k			AUTHOR		LDA			authorID
TASK6		k			AUTHOR		SVD			authorID
TASK6		k			AUTHOR		PCA			authorID
TASK6		k			PAPER		TF			paperID
TASK6		k			PAPER		TF-IDF		paperID

TASK8		k			authorID	KV
TASK8		k			authorID	LDA
TASK8		k			authorID	SVD
TASK8		k			authorID	PCA
TASK8		k			paperID		TF
TASK8		k			paperID		TF-IDF

k is a custom number
authorID is a custom author id
paperrID is a custom paper id