package fr.vdelll.quizappmvvm;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class QuizFragment extends Fragment implements View.OnClickListener {

    // Declare
    private static final String TAG = "LOG_QUIZ_FRAGMENT";
    private FirebaseFirestore firebaseFirestore;
    private String quizId;

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
    }

    /**
     * Load the UI
     */
    private void loadUI() {
        quizTitle.setText("Quiz data loaded");
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
        questionText.setText(questionsToAnswer.get(questNum).getQuestion());
        optionOneBtn.setText(questionsToAnswer.get(questNum).getOption_a());
        optionTwoBtn.setText(questionsToAnswer.get(questNum).getOption_b());
        optionThreeBtn.setText(questionsToAnswer.get(questNum).getOption_c());

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
        final Long timeToAnswer = questionsToAnswer.get(questionNumber).getTimer();
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
                answerSelected(optionOneBtn.getText());
                break;
            case R.id.quiz_option_two :
                answerSelected(optionTwoBtn.getText());
                break;
            case R.id.quiz_option_three :
                answerSelected(optionThreeBtn.getText());
                break;
        }
    }

    /**
     * Check answer
     *
     * @param selectedAnswer
     */
    private void answerSelected(CharSequence selectedAnswer) {

        if (canAnswer){
            if(questionsToAnswer.get(currentQuestion).getAnswer().equals(selectedAnswer)){
                // Correct answer
                Log.d(TAG, "answerSelected: correct");
            } else {
                // Wrong answer
                Log.d(TAG, "answerSelected: wrong");
            }
        }

    }
}