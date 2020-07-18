package fr.vdelll.quizappmvvm;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class QuizFragment extends Fragment implements View.OnClickListener {

    // Declare
    private static final String TAG = "LOG_QUIZ_FRAGMENT";
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;
    private NavController navController;
    private String quizId;
    private String quizName;
    private String currentUserId;

    // UI element
    private TextView quizTitle;
    private Button optionOneBtn;
    private Button optionTwoBtn;
    private Button optionThreeBtn;
    private Button nextBtn;
    private ImageButton closeBtn;
    private TextView questionFeedback;
    private TextView questionText;
    private TextView questionTime;
    private ProgressBar questionProgress;
    private TextView questionNumber;

    // Firebase data
    private List<QuestionsModel> allQuestionsList = new ArrayList<>();
    private long totalQuestionsToAnswer = 0L;
    private List<QuestionsModel> questionsToAnswer = new ArrayList<>();
    private CountDownTimer countDownTimer;

    private boolean canAnswer = false;
    private int currentQuestion = 0;

    private int correctAnswer = 0;
    private int wrongAnswer = 0;
    private int notAnswered = 0;

    public QuizFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_quiz, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        firebaseAuth = FirebaseAuth.getInstance();

        // Get User ID
        if (firebaseAuth.getCurrentUser() != null){
            currentUserId = firebaseAuth.getCurrentUser().getUid();
        } else {
            // Go back to home page
        }

        // Init
        firebaseFirestore = FirebaseFirestore.getInstance();

        // UI Elements
        quizTitle = view.findViewById(R.id.quiz_title);
        optionOneBtn = view.findViewById(R.id.quiz_option_one);
        optionTwoBtn = view.findViewById(R.id.quiz_option_two);
        optionThreeBtn = view.findViewById(R.id.quiz_option_three);
        nextBtn = view.findViewById(R.id.quiz_next_btn);
        questionFeedback = view.findViewById(R.id.quiz_question_feedback);
        questionText = view.findViewById(R.id.quiz_question);
        questionTime = view.findViewById(R.id.quiz_question_time);
        questionProgress = view.findViewById(R.id.quiz_question_progress);
        questionNumber = view.findViewById(R.id.quiz_question_number);

        // Get Quiz Id
        quizId = QuizFragmentArgs.fromBundle(getArguments()).getQuizId();
        quizName = QuizFragmentArgs.fromBundle(getArguments()).getQuizName();
        totalQuestionsToAnswer = QuizFragmentArgs.fromBundle(getArguments()).getTotalQuestions();

        // Get questions from quiz
        firebaseFirestore.collection("QuizList")
                .document(quizId)
                .collection("Questions")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            // Add all questions to list
                            allQuestionsList = task.getResult().toObjects(QuestionsModel.class);

                            // pick questions
                            pickQuestions();
                            loadUI();
                        } else {
                            // Error getting question
                            quizTitle.setText("Error loading data");
                        }
                    }
                });

        // Set on click listener
        optionOneBtn.setOnClickListener(this);
        optionTwoBtn.setOnClickListener(this);
        optionThreeBtn.setOnClickListener(this);

        nextBtn.setOnClickListener(this);
    }

    /**
     * Load the UI
     */
    private void loadUI() {
        quizTitle.setText(quizName);
        questionText.setText("Load first question");

        // enable options
        enableOptions();

        // Load first question
        loadQuestion(1);
    }

    /**
     * Chargement d'une question et des éléments associés
     *
     * @param questNum
     */
    private void loadQuestion(int questNum) {
        questionNumber.setText(questNum + "");
        questionText.setText(questionsToAnswer.get(questNum-1).getQuestion());
        optionOneBtn.setText(questionsToAnswer.get(questNum-1).getOption_a());
        optionTwoBtn.setText(questionsToAnswer.get(questNum-1).getOption_b());
        optionThreeBtn.setText(questionsToAnswer.get(questNum-1).getOption_c());

        // Question loaded, user can answer
        canAnswer = true;
        currentQuestion = questNum;

        startTimer(questNum);
    }

    /**
     * Set timer text and start countdown
     *
     * @param questionNumber
     */
    private void startTimer(int questionNumber) {
        final Long timeToAnswer = questionsToAnswer.get(questionNumber-1).getTimer();
        questionTime.setText(timeToAnswer.toString());

        // Show timer progress bar
        questionProgress.setVisibility(View.VISIBLE);

        countDownTimer = new CountDownTimer(timeToAnswer * 1000, 10) {

            @Override
            public void onTick(long l) {
                // text
                questionTime.setText(l / 1000 + "");

                // progress bar
                Long percent = l / (timeToAnswer * 10);
                questionProgress.setProgress(percent.intValue());
            }

            @Override
            public void onFinish() {
                canAnswer = false;
                questionFeedback.setText("Time Up ! No answer was submitted !");
                questionFeedback.setTextColor(getResources().getColor(R.color.colorPrimary, null));
                notAnswered++;
                showNextBtn();
            }
        };

        countDownTimer.start();
    }

    /**
     * Activation et désactivation des éléments UI
     */
    private void enableOptions() {
        // Show all option buttons
        optionOneBtn.setVisibility(View.VISIBLE);
        optionTwoBtn.setVisibility(View.VISIBLE);
        optionThreeBtn.setVisibility(View.VISIBLE);

        // Enable option button
        optionOneBtn.setEnabled(true);
        optionTwoBtn.setEnabled(true);
        optionThreeBtn.setEnabled(true);

        // Hide feedback and next button
        questionFeedback.setVisibility(View.INVISIBLE);
        nextBtn.setVisibility(View.INVISIBLE);
        nextBtn.setEnabled(false);
    }

    /**
     * Récupération de questions aléatoires
     */
    private void pickQuestions() {
        for (int i = 0; i < totalQuestionsToAnswer; i++) {
            int randomNumber = getRandomInteger(allQuestionsList.size(), 0);
            questionsToAnswer.add(allQuestionsList.get(randomNumber));
            allQuestionsList.remove(randomNumber);
            Log.d(TAG, "pickQuestions: " + questionsToAnswer.get(i).getQuestion());
        }
    }

    /**
     * Renvoie un entier aléatoire
     *
     * @param maximum
     * @param minimum
     * @return
     */
    public static int getRandomInteger(int maximum, int minimum) {
        return ((int) (Math.random() * (maximum - minimum))) + minimum;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.quiz_option_one :
                verifyAnswer(optionOneBtn);
                break;
            case R.id.quiz_option_two :
                verifyAnswer(optionTwoBtn);
                break;
            case R.id.quiz_option_three :
                verifyAnswer(optionThreeBtn);
                break;
            case R.id.quiz_next_btn :
                if (currentQuestion == totalQuestionsToAnswer){
                    // submit results
                    submitResults();
                } else {
                    currentQuestion++;
                    loadQuestion(currentQuestion);
                    resetOptions();
                }
                break;
        }
    }

    private void resetOptions() {
        optionOneBtn.setBackground(getResources().getDrawable(R.drawable.outline_light_btn_bg, null));
        optionTwoBtn.setBackground(getResources().getDrawable(R.drawable.outline_light_btn_bg, null));
        optionThreeBtn.setBackground(getResources().getDrawable(R.drawable.outline_light_btn_bg, null));

        optionOneBtn.setTextColor(getResources().getColor(R.color.colorLightText, null));
        optionTwoBtn.setTextColor(getResources().getColor(R.color.colorLightText, null));
        optionThreeBtn.setTextColor(getResources().getColor(R.color.colorLightText, null));

        questionFeedback.setVisibility(View.INVISIBLE);
        nextBtn.setVisibility(View.INVISIBLE);
        nextBtn.setEnabled(false);
    }

    /**
     * Check answer
     *
     * @param selectedAnswerBtn
     */
    private void verifyAnswer(Button selectedAnswerBtn) {

        if (canAnswer){

            selectedAnswerBtn.setTextColor(getResources().getColor(R.color.colorDark, null));

            if(questionsToAnswer.get(currentQuestion-1).getAnswer().equals(selectedAnswerBtn.getText())){
                // Correct answer
                correctAnswer++;
                selectedAnswerBtn.setBackground(getResources().getDrawable(R.drawable.correct_answer_btn_bg, null));

                questionFeedback.setText("Correct answer");
                questionFeedback.setTextColor(getResources().getColor(R.color.colorPrimary, null));

            } else {
                // Wrong answer
                wrongAnswer++;
                selectedAnswerBtn.setBackground(getResources().getDrawable(R.drawable.wrong_answer_btn_bg, null));

                questionFeedback.setText("Wrong answer \n\nCorrect answer : " + questionsToAnswer.get(currentQuestion-1).getAnswer());
                questionFeedback.setTextColor(getResources().getColor(R.color.colorAccent, null));
            }
            canAnswer = false;
            countDownTimer.cancel();
            showNextBtn();
        }

    }

    /**
     * Voir la réponse et le bouton pour la question suivante
     */
    private void showNextBtn() {

        if (currentQuestion == totalQuestionsToAnswer){
            nextBtn.setText("Submit results");
        }

        questionFeedback.setVisibility(View.VISIBLE);
        nextBtn.setVisibility(View.VISIBLE);
        nextBtn.setEnabled(true);
    }

    /**
     * Affichage des resultats
     */
    private void submitResults() {

        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("correct", correctAnswer);
        resultMap.put("wrong", wrongAnswer);
        resultMap.put("unanswered", notAnswered);

        firebaseFirestore.collection("QuizList")
                .document(quizId)
                .collection("Results")
                .document(currentUserId)
                .set(resultMap)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            // Go to result page
                            QuizFragmentDirections.ActionQuizFragmentToResultFragment action = QuizFragmentDirections.actionQuizFragmentToResultFragment();
                            action.setQuizId(quizId);
                            navController.navigate(action);
                        } else {
                            // show error
                            quizTitle.setText(task.getException().getMessage());
                        }
                    }
                });
    }
}