# DbClient
Main idea is to convert text associated with every item into info that could be used by machine learning algorithm (this project uses liblinear https://github.com/cjlin1/liblinear)  
To do so every word has been put through Porter stemmer and assigned an id.
Feature Node for ML is an Array of 1 and 0 where 1 means the word with id equal to place in array is present in text.
