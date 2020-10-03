module.exports = string => {
  const addGradeToResult = grade => {
    if (mg.cache.classes.hasOwnProperty(grade)) {
      for (let classEntity of mg.cache.classes[grade]) {
        result.push(classEntity.id);
      }
    }
  };

  const result = [];
  
  for (const range of string.split(';')) {
    if (range.indexOf('-') !== -1) {
      const [startGrade, endGrade] = range.split('-');

      for (startGrade; startGrade <= endGrade; startGrade += 1) {
        addGradeToResult(startGrade);
      }
    } else if (range.indexOf(',') !== -1) {
      const [grade, letter] = range.split(',');

      result.push(mg.cache.classes[grade][letter]);
    } else {
      addGradeToResult(range);
    }
  }

  return result;
};