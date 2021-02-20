import answer from 'protocol/answer/answer.json';
import positiveAnswerGeneral from 'protocol/answer/positiveAnswer/positiveAnswerGeneral.json';
import positiveAnswerCatchup from 'protocol/answer/positiveAnswer/positiveAnswerCatchup.json';
import negativeAnswer from 'protocol/answer/negativeAnswer/error.json';

const answerSchemas = [
  answer,
  positiveAnswerGeneral,
  positiveAnswerCatchup,
  negativeAnswer,
];

export default answerSchemas;
