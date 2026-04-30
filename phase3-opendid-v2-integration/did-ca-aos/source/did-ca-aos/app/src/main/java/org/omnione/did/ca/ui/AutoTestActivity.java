/*
 * Copyright 2025 OmniOne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.omnione.did.ca.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;
import org.omnione.did.ca.config.Config;
import org.omnione.did.ca.config.Constants;
import org.omnione.did.ca.config.Preference;
import org.omnione.did.ca.logger.CaLog;
import org.omnione.did.ca.network.HttpUrlConnection;
import org.omnione.did.ca.network.protocol.BaseOperation;
import org.omnione.did.ca.network.protocol.ProtocolData;
import org.omnione.did.ca.network.protocol.UserRegistration;
import org.omnione.did.ca.network.protocol.VcIssuance;
import org.omnione.did.ca.network.protocol.VpSubmission;
import org.omnione.did.sdk.core.api.WalletApi;
import org.omnione.did.sdk.core.exception.WalletCoreException;
import org.omnione.did.sdk.datamodel.did.DIDDocument;
import org.omnione.did.sdk.datamodel.did.SignedDidDoc;
import org.omnione.did.sdk.utility.CryptoUtils;
import org.omnione.did.sdk.utility.DataModels.EcKeyPair;
import org.omnione.did.sdk.utility.DataModels.EcType;
import org.omnione.did.sdk.utility.DataModels.MultibaseType;
import org.omnione.did.sdk.utility.DataModels.DigestEnum;
import org.omnione.did.sdk.utility.DigestUtils;
import org.omnione.did.sdk.utility.Errors.UtilityException;
import org.omnione.did.sdk.utility.MultibaseUtils;
import org.omnione.did.sdk.wallet.walletservice.exception.WalletException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigInteger;
import java.util.concurrent.ExecutionException;
import org.spongycastle.crypto.params.ECDomainParameters;
import org.spongycastle.crypto.params.ECPublicKeyParameters;
import org.spongycastle.crypto.signers.ECDSASigner;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec;

/**
 * Layer 2 자동화 테스트 Activity.
 *
 * 모드 선택 → Scenario A-D 구성 화면으로 진입한다.
 *   A: 전체 흐름 반복 (REG → VC 발급 → VP 제출, 5/50/100 클레임 선택)
 *   B: VC 발급 클레임 수 테스트 (P132 1회 사전 등록 + P210 반복, 5/50/100 클레임 선택)
 *   C: VP 제출 클레임 수 테스트 (P132 + P210 사전 1회 후 P310 반복, 5/50/100 클레임 선택)
 *   D: 키교환 단독 테스트 (ECDH vs ML-KEM-768)
 * 테스트 종료 또는 화면 종료 시 SDK KEY_ALGORITHM을 PQC(MlDsa44)로 복원한다.
 *
 * PIN / VP Policy ID 설정: app/build.gradle AUTO_TEST_PIN, AUTO_TEST_VP_POLICY_ID_5/50/100 필드
 *
 * adb 실행 예시:
 *   adb shell am start -n org.omnione.did.ca/.ui.AutoTestActivity --es mode A
 *   adb shell am start -n org.omnione.did.ca/.ui.AutoTestActivity --es mode B
 *   adb shell am start -n org.omnione.did.ca/.ui.AutoTestActivity --es mode C
 *   adb shell am start -n org.omnione.did.ca/.ui.AutoTestActivity --es mode D
 *
 * Logcat 수집: adb logcat -s PQC_PERF  (Scenario D: PQC_PERF_D)
 */
public class AutoTestActivity extends AppCompatActivity {

    private static final String TAG = "PQC_PERF";
    private static final String TAG_C = "PQC_PERF_C";
    private static final String TAG_D = "PQC_PERF_D";
    private static final String AUTO_PIN = Config.AutoTest.PIN;
    private static final String SDK_ALGO_DEFAULT = "MlDsa44";
    private static final String TEST_CLIENT_DID = "did:omn:test-client";
    private static final int A_MAX_CONSECUTIVE_ERRORS = 5;
    private static final int D_MAX_CONSECUTIVE_ERRORS = 5;

    private static final int DEFAULT_WARMUP = 0;
    private static final int DEFAULT_MEASUREMENT = 1;
    private static final int WARMUP_MIN = 0;
    private static final int WARMUP_MAX = 20;
    private static final int MEASUREMENT_MIN = 1;
    private static final int MEASUREMENT_MAX = 50;

    // Scenario A config UI
    private RadioGroup algorithmGroup;
    private RadioButton pqcButton;
    private RadioButton classicButton;
    private RadioGroup vpPlanGroupA;
    private Button startButton;
    private TextView warmupCountView;
    private TextView measurementCountView;

    // status output
    private TextView statusView;
    private ScrollView scrollView;

    // running guard
    private boolean isRunning = false;

    // test config
    private int warmupCount = DEFAULT_WARMUP;
    private int measurementCount = DEFAULT_MEASUREMENT;
    private String selectedAlgorithm = SDK_ALGO_DEFAULT;
    private String selectedVcPlanIdA = PERF_VC_PLAN_IDS[0];
    private String selectedVpPolicyIdA = PERF_VP_POLICY_IDS[0];

    // timing results (measurement phase only) [regNs, issueNs, vpNs]
    private final List<long[]> results = new ArrayList<>();

    // Scenario D (키교환 단독) state
    private RadioGroup algorithmGroupD;
    private RadioButton ecdhButtonD;
    private RadioButton mlKemButtonD;
    private Button startButtonD;
    private boolean useMlKem = true;
    private final List<long[]> dResults = new ArrayList<>();

    // Scenario A consecutive error counter
    private int aConsecErrors = 0;

    // VC plan / VP policy ID arrays (indexed in sync)
    private static final String[] PERF_VC_PLAN_IDS    = {"perf.claims.5", "perf.claims.50", "perf.claims.100"};
    private static final String[] PERF_VP_POLICY_IDS  = {
        Config.AutoTest.VP_POLICY_ID_5,
        Config.AutoTest.VP_POLICY_ID_50,
        Config.AutoTest.VP_POLICY_ID_100
    };

    // Scenario B state
    private static final int B_MAX_CONSECUTIVE_ERRORS = 5;
    private RadioGroup algorithmGroupB;
    private RadioButton pqcButtonB;
    private RadioButton classicButtonB;
    private RadioGroup vcPlanGroupB;
    private Button startButtonB;
    private String selectedVcPlanIdB = PERF_VC_PLAN_IDS[0];
    private final List<long[]> bResults = new ArrayList<>();
    private int bConsecErrors = 0;

    // Scenario C (VP 제출 클레임 수) state
    private static final int C_MAX_CONSECUTIVE_ERRORS = 5;
    private RadioGroup algorithmGroupC;
    private RadioButton pqcButtonC;
    private RadioButton classicButtonC;
    private RadioGroup vpPlanGroupC;
    private Button startButtonC;
    private String selectedVcPlanIdC  = PERF_VC_PLAN_IDS[0];
    private String selectedVpPolicyIdC = Config.AutoTest.VP_POLICY_ID_5;
    private final List<long[]> cResults = new ArrayList<>();  // [totalNs, serverNs, appNs]
    private int cConsecErrors = 0;

    // ────────────────────────────────────────────────
    // 생명주기
    // ────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Legacy support: iterations extra from older launches → use as measurementCount
        int legacyIterations = getIntent().getIntExtra("iterations", -1);
        if (legacyIterations > 0) {
            measurementCount = Math.max(MEASUREMENT_MIN, Math.min(MEASUREMENT_MAX, legacyIterations));
        }

        String mode = getIntent().getStringExtra("mode");
        if ("A".equals(mode)) {
            showScenarioAConfig();
        } else if ("B".equals(mode)) {
            showScenarioBConfig();
        } else if ("C".equals(mode)) {
            showScenarioCConfig();
        } else if ("D".equals(mode)) {
            showScenarioDConfig();
        } else {
            showModeSelection();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 일반 앱 로직 보호: 화면을 벗어날 때 항상 PQC 모드로 복원
        org.omnione.did.sdk.wallet.walletservice.config.Config.KEY_ALGORITHM = SDK_ALGO_DEFAULT;
        Log.i(TAG, "onDestroy: KEY_ALGORITHM restored to " + SDK_ALGO_DEFAULT);
    }

    @Override
    public void onBackPressed() {
        if (isRunning) {
            Log.i(TAG, "Back press blocked: test in progress");
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (isRunning) {
                return true;
            }
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ────────────────────────────────────────────────
    // 화면 디스패치
    // ────────────────────────────────────────────────

    private void setActionBarTitle(String title, boolean showBack) {
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setTitle(title);
            bar.setDisplayHomeAsUpEnabled(showBack);
        }
    }

    private void showModeSelection() {
        setActionBarTitle("자동화 테스트", false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 64, 48, 32);

        TextView title = new TextView(this);
        title.setText("모드 선택");
        title.setTextSize(20f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 32);
        root.addView(title);

        root.addView(buildModeCard(
                "Scenario A — 전체 흐름 반복 테스트",
                "REG → VC 발급 → VP 제출",
                this::showScenarioAConfig));

        View spacer = new View(this);
        LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 24);
        root.addView(spacer, spacerLp);

        View spacer2 = new View(this);
        LinearLayout.LayoutParams spacer2Lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 24);
        root.addView(spacer2, spacer2Lp);

        root.addView(buildModeCard(
                "Scenario B — VC 발급 클레임 수 테스트",
                "P132 1회 사전 등록 + P210 반복  (5 / 50 / 100 클레임)",
                this::showScenarioBConfig));

        View spacer3 = new View(this);
        LinearLayout.LayoutParams spacer3Lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 24);
        root.addView(spacer3, spacer3Lp);

        root.addView(buildModeCard(
                "Scenario C — VP 제출 클레임 수 테스트",
                "P132 + P210 사전 1회 + P310 반복  (5 / 50 / 100 클레임)",
                this::showScenarioCConfig));

        View spacer4 = new View(this);
        LinearLayout.LayoutParams spacer4Lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 24);
        root.addView(spacer4, spacer4Lp);

        root.addView(buildModeCard(
                "Scenario D — 키교환 단독 테스트",
                "ECDH vs ML-KEM-768",
                this::showScenarioDConfig));

        setContentView(root);
    }

    private View buildModeCard(String title, String subtitle, Runnable onClick) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(36, 36, 36, 36);
        card.setBackgroundColor(0xFFF2F4F7);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> onClick.run());

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(16f);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(t);

        TextView s = new TextView(this);
        s.setText(subtitle);
        s.setTextSize(13f);
        s.setPadding(0, 8, 0, 0);
        card.addView(s);

        return card;
    }

    // ────────────────────────────────────────────────
    // Scenario A 구성 화면
    // ────────────────────────────────────────────────

    private void showScenarioAConfig() {
        setActionBarTitle("Scenario A — 전체 흐름", true);
        // 기존 테스트 결과 UI 초기화
        results.clear();
        isRunning = false;
        aConsecErrors = 0;
        selectedVcPlanIdA = PERF_VC_PLAN_IDS[0];
        selectedVpPolicyIdA = PERF_VP_POLICY_IDS[0];
        setContentView(buildScenarioALayout());
    }

    private View buildScenarioALayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 64, 48, 32);

        TextView title = new TextView(this);
        title.setText("PQC 성능 테스트");
        title.setTextSize(20f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 32);
        root.addView(title);

        // 알고리즘 선택
        TextView algoLabel = new TextView(this);
        algoLabel.setText("알고리즘 선택");
        algoLabel.setTextSize(14f);
        algoLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        algoLabel.setPadding(0, 0, 0, 8);
        root.addView(algoLabel);

        algorithmGroup = new RadioGroup(this);
        algorithmGroup.setOrientation(RadioGroup.VERTICAL);

        pqcButton = new RadioButton(this);
        pqcButton.setId(View.generateViewId());
        pqcButton.setText("PQC  (ML-DSA-44)");
        pqcButton.setChecked(true);
        pqcButton.setTextSize(14f);

        classicButton = new RadioButton(this);
        classicButton.setId(View.generateViewId());
        classicButton.setText("Classic  (Secp256r1)");
        classicButton.setTextSize(14f);

        algorithmGroup.addView(pqcButton);
        algorithmGroup.addView(classicButton);
        algorithmGroup.setPadding(0, 4, 0, 24);
        root.addView(algorithmGroup);

        TextView planLabel = new TextView(this);
        planLabel.setText("클레임 수 선택");
        planLabel.setTextSize(14f);
        planLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        planLabel.setPadding(0, 0, 0, 8);
        root.addView(planLabel);

        vpPlanGroupA = new RadioGroup(this);
        vpPlanGroupA.setOrientation(RadioGroup.VERTICAL);

        String[] planLabels = {"5 클레임  (perf.claims.5)", "50 클레임  (perf.claims.50)", "100 클레임  (perf.claims.100)"};
        for (int i = 0; i < PERF_VC_PLAN_IDS.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setId(View.generateViewId());
            rb.setText(planLabels[i]);
            rb.setTextSize(14f);
            rb.setChecked(i == 0);
            final int idx = i;
            rb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedVcPlanIdA = PERF_VC_PLAN_IDS[idx];
                    selectedVpPolicyIdA = PERF_VP_POLICY_IDS[idx];
                }
            });
            vpPlanGroupA.addView(rb);
        }
        vpPlanGroupA.setPadding(0, 4, 0, 24);
        root.addView(vpPlanGroupA);

        // Warmup stepper
        root.addView(buildStepperRow(
                "Warmup (0-20)",
                warmupCount,
                WARMUP_MIN, WARMUP_MAX,
                true));

        // Measurement stepper
        root.addView(buildStepperRow(
                "Measurement (1-50)",
                measurementCount,
                MEASUREMENT_MIN, MEASUREMENT_MAX,
                false));

        // vcPlanId / policyId / PIN info
        TextView infoView = new TextView(this);
        infoView.setText("vcPlanId: " + selectedVcPlanIdA
                + "\npolicyId: " + selectedVpPolicyIdA
                + "\nPIN: " + AUTO_PIN);
        infoView.setTextSize(12f);
        infoView.setPadding(0, 8, 0, 24);
        root.addView(infoView);

        // 시작 버튼
        startButton = new Button(this);
        startButton.setText("테스트 시작");
        startButton.setOnClickListener(v -> startTest());
        root.addView(startButton);

        // 구분선
        View divider = new View(this);
        divider.setBackgroundColor(0xFFCCCCCC);
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2);
        divParams.setMargins(0, 32, 0, 16);
        root.addView(divider, divParams);

        // 상태 출력
        scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        statusView = new TextView(this);
        statusView.setPadding(0, 8, 0, 0);
        statusView.setTextSize(12f);
        scrollView.addView(statusView);
        root.addView(scrollView, scrollParams);

        return root;
    }

    private View buildStepperRow(String label, int initial, int min, int max, boolean isWarmup) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, 16);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextSize(13f);
        LinearLayout.LayoutParams lblLp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(lbl, lblLp);

        Button minus = new Button(this);
        minus.setText("−");
        row.addView(minus);

        TextView count = new TextView(this);
        count.setText(String.valueOf(initial));
        count.setTextSize(16f);
        count.setTypeface(null, android.graphics.Typeface.BOLD);
        count.setPadding(32, 0, 32, 0);
        count.setMinWidth(96);
        count.setGravity(Gravity.CENTER);
        row.addView(count);

        Button plus = new Button(this);
        plus.setText("+");
        row.addView(plus);

        if (isWarmup) {
            warmupCountView = count;
        } else {
            measurementCountView = count;
        }

        minus.setOnClickListener(v -> {
            int current = parseCount(count, initial);
            int next = Math.max(min, current - 1);
            count.setText(String.valueOf(next));
            if (isWarmup) warmupCount = next; else measurementCount = next;
        });
        plus.setOnClickListener(v -> {
            int current = parseCount(count, initial);
            int next = Math.min(max, current + 1);
            count.setText(String.valueOf(next));
            if (isWarmup) warmupCount = next; else measurementCount = next;
        });

        return row;
    }

    private int parseCount(TextView view, int fallback) {
        try {
            return Integer.parseInt(view.getText().toString().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ────────────────────────────────────────────────
    // Scenario C 구성 화면
    // ────────────────────────────────────────────────

    private void showScenarioDConfig() {
        setActionBarTitle("Scenario D — 키교환", true);
        isRunning = false;
        dResults.clear();
        setContentView(buildScenarioDLayout());
    }

    private View buildScenarioDLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 64, 48, 32);

        TextView title = new TextView(this);
        title.setText("Scenario D — 키교환 단독 테스트");
        title.setTextSize(20f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 32);
        root.addView(title);

        TextView algoLabel = new TextView(this);
        algoLabel.setText("알고리즘 선택");
        algoLabel.setTextSize(14f);
        algoLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        algoLabel.setPadding(0, 0, 0, 8);
        root.addView(algoLabel);

        algorithmGroupD = new RadioGroup(this);
        algorithmGroupD.setOrientation(RadioGroup.VERTICAL);

        ecdhButtonD = new RadioButton(this);
        ecdhButtonD.setId(View.generateViewId());
        ecdhButtonD.setText("ECDH (Secp256r1)");
        ecdhButtonD.setTextSize(14f);
        ecdhButtonD.setChecked(!useMlKem);

        mlKemButtonD = new RadioButton(this);
        mlKemButtonD.setId(View.generateViewId());
        mlKemButtonD.setText("ML-KEM-768");
        mlKemButtonD.setTextSize(14f);
        mlKemButtonD.setChecked(useMlKem);

        algorithmGroupD.addView(ecdhButtonD);
        algorithmGroupD.addView(mlKemButtonD);
        algorithmGroupD.setPadding(0, 4, 0, 24);
        root.addView(algorithmGroupD);

        root.addView(buildStepperRow(
                "Warmup (0-20)",
                warmupCount,
                WARMUP_MIN, WARMUP_MAX,
                true));

        root.addView(buildStepperRow(
                "Measurement (1-50)",
                measurementCount,
                MEASUREMENT_MIN, MEASUREMENT_MAX,
                false));

        TextView infoView = new TextView(this);
        infoView.setText("엔드포인트: " + Config.TAS.REQUEST_ECDH_ONLY);
        infoView.setTextSize(12f);
        infoView.setPadding(0, 8, 0, 24);
        root.addView(infoView);

        startButtonD = new Button(this);
        startButtonD.setText("시작");
        startButtonD.setOnClickListener(v -> startTestD());
        root.addView(startButtonD);

        View divider = new View(this);
        divider.setBackgroundColor(0xFFCCCCCC);
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2);
        divParams.setMargins(0, 32, 0, 16);
        root.addView(divider, divParams);

        scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        statusView = new TextView(this);
        statusView.setPadding(0, 8, 0, 0);
        statusView.setTextSize(12f);
        statusView.setTypeface(android.graphics.Typeface.MONOSPACE);
        scrollView.addView(statusView);
        root.addView(scrollView, scrollParams);

        return root;
    }

    // ────────────────────────────────────────────────
    // Scenario C 테스트 실행
    // ────────────────────────────────────────────────

    private void startTestD() {
        warmupCount = clamp(parseCount(warmupCountView, DEFAULT_WARMUP), WARMUP_MIN, WARMUP_MAX);
        measurementCount = clamp(parseCount(measurementCountView, DEFAULT_MEASUREMENT),
                MEASUREMENT_MIN, MEASUREMENT_MAX);
        useMlKem = mlKemButtonD.isChecked();
        String algoLabel = useMlKem ? "ML-KEM-768" : "ECDH";

        ecdhButtonD.setEnabled(false);
        mlKemButtonD.setEnabled(false);
        algorithmGroupD.setEnabled(false);
        startButtonD.setEnabled(false);

        dResults.clear();
        setRunning(true);

        setStatus("Scenario D 시작\n알고리즘: " + algoLabel
                + "\nWarmup: " + warmupCount + "회 | Measurement: " + measurementCount + "회"
                + "\n엔드포인트: " + Config.TAS.REQUEST_ECDH_ONLY + "\n");
        Log.i(TAG_D, "=== Scenario D Start | algo=" + algoLabel
                + " | warmup=" + warmupCount + " | measurement=" + measurementCount + " ===");

        new Thread(this::runKeyExchangeLoop).start();
    }

    private void runKeyExchangeLoop() {
        int consecutiveErrors = 0;

        for (int i = 0; i < warmupCount; i++) {
            boolean ok = runKeyExchangeIteration(i, true);
            consecutiveErrors = ok ? 0 : consecutiveErrors + 1;
            if (consecutiveErrors >= D_MAX_CONSECUTIVE_ERRORS) {
                abortScenarioD(consecutiveErrors);
                return;
            }
        }
        if (warmupCount > 0) {
            Log.i(TAG_D, "=== WARMUP DONE, entering MEASUREMENT ===");
            appendStatus("\n[WARMUP DONE]");
        }

        for (int i = 0; i < measurementCount; i++) {
            boolean ok = runKeyExchangeIteration(i, false);
            consecutiveErrors = ok ? 0 : consecutiveErrors + 1;
            if (consecutiveErrors >= D_MAX_CONSECUTIVE_ERRORS) {
                abortScenarioD(consecutiveErrors);
                return;
            }
        }

        runOnUiThread(this::showScenarioDReport);
    }

    private boolean runKeyExchangeIteration(int index, boolean isWarmup) {
        String tag = iterTagD(index, isWarmup);
        try {
            // 1. 클라이언트 임시 키페어 생성 (타이밍 외부 — 서버와 동일한 방식으로 분리)
            byte[] kemPrivateKey = null;
            byte[] ecPrivateKey = null;
            String publicKey;
            if (useMlKem) {
                CryptoUtils.MlKemKeyPair kp = CryptoUtils.generateMLKEMKeyPair();
                kemPrivateKey = kp.getPrivateKey();
                publicKey = MultibaseUtils.encode(
                        MultibaseType.MULTIBASE_TYPE.BASE_58_BTC, kp.getPublicKey());
            } else {
                EcKeyPair kp = CryptoUtils.generateECKeyPair(EcType.EC_TYPE.SECP256_R1);
                ecPrivateKey = MultibaseUtils.decode(kp.getPrivateKey());
                publicKey = kp.getPublicKey();
            }

            byte[] nonce = CryptoUtils.generateNonce(16);
            String clientNonce = MultibaseUtils.encode(
                    MultibaseType.MULTIBASE_TYPE.BASE_58_BTC, nonce);

            JSONObject body = new JSONObject();
            body.put("client", TEST_CLIENT_DID);
            body.put("clientNonce", clientNonce);
            if (useMlKem) {
                body.put("algorithm", "ML-KEM-768");
            } else {
                body.put("curve", "Secp256r1");
            }
            body.put("publicKey", publicKey);

            // 2. HTTP 요청/응답 시간 측정
            long httpStart = System.nanoTime();
            String resp = new HttpUrlConnection()
                    .send(this, Config.TAS.REQUEST_ECDH_ONLY, "POST", body.toString());
            long httpEnd = System.nanoTime();

            if (resp == null) {
                appendStatus(tag + " [ERROR iter " + (index + 1) + "] HTTP 응답 없음");
                Log.e(TAG_D, tag + " HTTP response is null");
                return false;
            }

            // 3. 응답 파싱
            JSONObject respJson;
            double serverProcessingMs;
            String ciphertextStr;
            String serverPublicKeyStr;
            String proofValueStr;
            String tasKeyAgreePublicKeyStr;
            try {
                respJson = new JSONObject(resp);
                serverProcessingMs = respJson.getDouble("serverProcessingMs");
                ciphertextStr = respJson.isNull("ciphertext") ? null : respJson.optString("ciphertext", null);
                serverPublicKeyStr = respJson.isNull("serverPublicKey") ? null : respJson.optString("serverPublicKey", null);
                proofValueStr = respJson.getString("proofValue");
                tasKeyAgreePublicKeyStr = respJson.getString("tasKeyAgreePublicKey");
            } catch (org.json.JSONException e) {
                throw new Exception("서버 응답 파싱 실패: " + e.getMessage());
            }

            // 4. 클라이언트 크립토 시간 측정: 키교환(decapsulate/ECDH) + TAS 서명 검증
            long clientCryptoStart = System.nanoTime();

            if (useMlKem) {
                byte[] ciphertextBytes = MultibaseUtils.decode(ciphertextStr);
                CryptoUtils.mlKemDecapsulate(kemPrivateKey, ciphertextBytes);
            } else {
                byte[] serverPubKeyBytes = MultibaseUtils.decode(serverPublicKeyStr);
                CryptoUtils.generateSharedSecret(EcType.EC_TYPE.SECP256_R1, ecPrivateKey, serverPubKeyBytes);
            }

            // ML-DSA-44: 원본 데이터로 검증 (서명 크기 > 65바이트)
            // ECDSA:     SHA-256 해싱 후 검증 (서명 크기 65바이트 = recoveryId + r||s)
            byte[] tasPublicKeyBytes = MultibaseUtils.decode(tasKeyAgreePublicKeyStr);
            byte[] signatureBytes    = MultibaseUtils.decode(proofValueStr);
            byte[] verifyData = (signatureBytes.length > 65)
                    ? new byte[32]  // ML-DSA-44: 원본 그대로
                    : DigestUtils.getDigest(new byte[32], DigestEnum.DIGEST_ENUM.SHA_256);  // ECDSA: SHA-256 해싱
            verifyTasSignature(tasPublicKeyBytes, verifyData, signatureBytes);

            long clientCryptoNs = System.nanoTime() - clientCryptoStart;

            // 5. 결과 집계
            long roundtripNs = httpEnd - httpStart;
            long serverNs    = (long)(serverProcessingMs * 1_000_000);
            long networkNs   = Math.max(0, roundtripNs - serverNs);

            if (!isWarmup) {
                dResults.add(new long[]{roundtripNs, serverNs, networkNs, clientCryptoNs});
            }

            String line = String.format(
                    "%s roundtrip=%dms server=%dms network=%dms clientCrypto=%dms",
                    tag, ns2ms(roundtripNs), ns2ms(serverNs), ns2ms(networkNs), ns2ms(clientCryptoNs));
            appendStatus(line);
            Log.i(TAG_D, line);
            return true;

        } catch (Exception e) {
            String msg = tag + " [ERROR iter " + (index + 1) + "] " + e.getClass().getSimpleName()
                    + ": " + e.getMessage();
            appendStatus(msg);
            Log.e(TAG_D, msg);
            return false;
        }
    }

    /**
     * TAS 서명 알고리즘 자동 판별 후 검증.
     *
     * - sigBytes.length == 64 : ECDSA P-256 compact (classic 모드)
     * - 그 외               : ML-DSA-44 (PQC 모드, 서명 크기 2420바이트)
     *
     * @param pubKeyBytes  TAS keyagree 공개키 (classic: 33-byte 압축 EC점, PQC: SubjectPublicKeyInfo DER)
     * @param message      서명된 원본 메시지 (dummySignatureMessage = new byte[32])
     * @param sigBytes     서명 바이트
     */
    /**
     * TAS 서명 알고리즘 자동 판별 후 검증.
     *
     * - sigBytes.length == 65 : ECDSA P-256 compact (recoveryId + r||s) — classic 모드
     * - 그 외                 : ML-DSA-44 — PQC 모드 (서명 크기 2420바이트)
     */
    private static void verifyTasSignature(byte[] pubKeyBytes, byte[] message, byte[] sigBytes) throws Exception {
        if (sigBytes.length == 65) {
            // Classic: ECDSA P-256 compact (1-byte recoveryId + 32R + 32S = 65바이트)
            verifyCompactEcdsaSecp256r1(pubKeyBytes, message, sigBytes);
        } else {
            // PQC: ML-DSA-44
            try {
                boolean ok = org.omnione.did.sdk.core.keymanager.supportalgorithm.MlDsa44SignatureCompat
                        .verify(pubKeyBytes, message, sigBytes);
                if (!ok) {
                    throw new Exception("TAS ML-DSA-44 서명 검증 실패");
                }
            } catch (WalletCoreException e) {
                throw new Exception("TAS ML-DSA-44 서명 검증 오류: " + e.getMessage());
            }
        }
    }

    /**
     * SpongyCastle ECDSASigner로 P-256 compact ECDSA 서명 검증.
     * compact 포맷: [recoveryId(1) || R(32) || S(32)] = 65바이트
     */
    private static void verifyCompactEcdsaSecp256r1(byte[] compressedPubKey,
                                                     byte[] messageHash,
                                                     byte[] compactSig) throws Exception {
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256r1");
        ECDomainParameters domainParams = new ECDomainParameters(
                spec.getCurve(), spec.getG(), spec.getN(), spec.getH());
        org.spongycastle.math.ec.ECPoint point = spec.getCurve().decodePoint(compressedPubKey);
        ECPublicKeyParameters pubKeyParams = new ECPublicKeyParameters(point, domainParams);

        // recoveryId(1바이트) 건너뛰고 R, S 추출
        BigInteger r = new BigInteger(1, Arrays.copyOfRange(compactSig, 1, 33));
        BigInteger s = new BigInteger(1, Arrays.copyOfRange(compactSig, 33, 65));

        ECDSASigner signer = new ECDSASigner();
        signer.init(false, pubKeyParams);
        if (!signer.verifySignature(messageHash, r, s)) {
            throw new Exception("TAS ECDSA P-256 서명 검증 실패");
        }
    }

    private void abortScenarioD(int consecutiveErrors) {
        Log.e(TAG_D, "=== Scenario D ABORT: consecutive errors=" + consecutiveErrors + " ===");
        runOnUiThread(() -> {
            appendStatus("\n[중단] 연속 오류 " + consecutiveErrors + "회 — 테스트를 중단합니다.");
            setRunning(false);
            if (startButtonD != null) {
                startButtonD.setEnabled(true);
                startButtonD.setText("다시 테스트");
                startButtonD.setOnClickListener(v -> showScenarioDConfig());
            }
        });
    }

    private void abortScenarioA() {
        Log.e(TAG, "=== Scenario A ABORT: consecutive errors=" + aConsecErrors + " ===");
        runOnUiThread(() -> {
            appendStatus("[ABORT] 연속 오류 " + aConsecErrors + "회 발생. 테스트 중단.");
            setRunning(false);
        });
    }

    private String iterTagD(int index, boolean isWarmup) {
        if (isWarmup) {
            return "[WARMUP " + (index + 1) + "/" + warmupCount + "]";
        }
        return "[MEASURE " + (index + 1) + "/" + measurementCount + "]";
    }

    // ────────────────────────────────────────────────
    // Scenario C 리포트
    // ────────────────────────────────────────────────

    private void showScenarioDReport() {
        setRunning(false);

        String algoLabel = useMlKem ? "ML-KEM-768" : "ECDH";

        if (dResults.isEmpty()) {
            appendStatus("\n[리포트] 성공한 측정 반복이 없습니다.");
            Log.i(TAG_D, "=== REPORT: no measurement results ===");
            if (startButtonD != null) {
                startButtonD.setEnabled(true);
                startButtonD.setText("다시 테스트");
                startButtonD.setOnClickListener(v -> showScenarioDConfig());
            }
            return;
        }

        int n = dResults.size();
        long[] roundtrip    = new long[n];
        long[] server       = new long[n];
        long[] network      = new long[n];
        long[] clientCrypto = new long[n];
        for (int i = 0; i < n; i++) {
            long[] r = dResults.get(i);
            roundtrip[i]    = r[0];
            server[i]       = r[1];
            network[i]      = r[2];
            clientCrypto[i] = r[3];
        }
        Arrays.sort(roundtrip);
        Arrays.sort(server);
        Arrays.sort(network);
        Arrays.sort(clientCrypto);

        String report = String.format(
                "\n========== Scenario D Report ==========\n" +
                "알고리즘: %s\n" +
                "Warmup: %d회 (제외) | Measurement: %d회\n\n" +
                "%-6s %14s %14s %14s %16s\n" +
                "%-6s %14s %14s %14s %16s\n" +
                "%-6s %14s %14s %14s %16s\n" +
                "%-6s %14s %14s %14s %16s\n" +
                "%-6s %14s %14s %14s %16s\n" +
                "%-6s %14s %14s %14s %16s\n" +
                "=======================================",
                algoLabel, warmupCount, n,
                "Metric", "Roundtrip(ms)",          "Server(ms)",           "Network(ms)",          "ClientCrypto(ms)",
                "mean",   msFmt(mean(roundtrip)),    msFmt(mean(server)),    msFmt(mean(network)),   msFmt(mean(clientCrypto)),
                "p50",    msFmt(calcP50(roundtrip)), msFmt(calcP50(server)), msFmt(calcP50(network)),msFmt(calcP50(clientCrypto)),
                "p95",    msFmt(calcP95(roundtrip)), msFmt(calcP95(server)), msFmt(calcP95(network)),msFmt(calcP95(clientCrypto)),
                "min",    msFmt(roundtrip[0]),       msFmt(server[0]),       msFmt(network[0]),      msFmt(clientCrypto[0]),
                "max",    msFmt(roundtrip[n - 1]),   msFmt(server[n - 1]),   msFmt(network[n - 1]),  msFmt(clientCrypto[n - 1])
        );
        appendStatus(report);
        Log.i(TAG_D, report.replace("\n", " | "));
        Log.i(TAG_D, String.format(
                "SUMMARY algo=%s n=%d warmup=%d "
                        + "meanRoundtrip=%.2fms meanServer=%.2fms meanNetwork=%.2fms meanClientCrypto=%.2fms "
                        + "p95Roundtrip=%.2fms p95Server=%.2fms p95ClientCrypto=%.2fms",
                algoLabel, n, warmupCount,
                ns2msD(mean(roundtrip)), ns2msD(mean(server)), ns2msD(mean(network)), ns2msD(mean(clientCrypto)),
                ns2msD(calcP95(roundtrip)), ns2msD(calcP95(server)), ns2msD(calcP95(clientCrypto))));

        java.util.LinkedHashMap<String, long[]> metrics = new java.util.LinkedHashMap<>();
        metrics.put("roundtrip",    roundtrip);
        metrics.put("server",       server);
        metrics.put("network",      network);
        metrics.put("clientCrypto", clientCrypto);
        sendReportToObserver("D", algoLabel, null, null, n, metrics);

        if (startButtonD != null) {
            startButtonD.setEnabled(true);
            startButtonD.setText("다시 테스트");
            startButtonD.setOnClickListener(v -> showScenarioDConfig());
        }
    }

    private static double ns2msD(long ns) {
        return ns / 1_000_000.0;
    }

    private static String msFmt(long ns) {
        return String.format("%.2f", ns2msD(ns));
    }

    // ────────────────────────────────────────────────
    // Scenario B 구성 화면
    // ────────────────────────────────────────────────

    private void showScenarioBConfig() {
        setActionBarTitle("Scenario B — VC 발급 클레임 수", true);
        bResults.clear();
        bConsecErrors = 0;
        isRunning = false;
        selectedVcPlanIdB = PERF_VC_PLAN_IDS[0];
        setContentView(buildScenarioBLayout());
    }

    private View buildScenarioBLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 64, 48, 32);

        TextView title = new TextView(this);
        title.setText("Scenario B — VC 발급 클레임 수 테스트");
        title.setTextSize(20f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 32);
        root.addView(title);

        TextView algoLabel = new TextView(this);
        algoLabel.setText("알고리즘 선택");
        algoLabel.setTextSize(14f);
        algoLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        algoLabel.setPadding(0, 0, 0, 8);
        root.addView(algoLabel);

        algorithmGroupB = new RadioGroup(this);
        algorithmGroupB.setOrientation(RadioGroup.VERTICAL);

        pqcButtonB = new RadioButton(this);
        pqcButtonB.setId(View.generateViewId());
        pqcButtonB.setText("PQC  (ML-DSA-44)");
        pqcButtonB.setChecked(true);
        pqcButtonB.setTextSize(14f);

        classicButtonB = new RadioButton(this);
        classicButtonB.setId(View.generateViewId());
        classicButtonB.setText("Classic  (Secp256r1)");
        classicButtonB.setTextSize(14f);

        algorithmGroupB.addView(pqcButtonB);
        algorithmGroupB.addView(classicButtonB);
        algorithmGroupB.setPadding(0, 4, 0, 24);
        root.addView(algorithmGroupB);

        TextView planLabel = new TextView(this);
        planLabel.setText("VC 클레임 수 선택");
        planLabel.setTextSize(14f);
        planLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        planLabel.setPadding(0, 0, 0, 8);
        root.addView(planLabel);

        vcPlanGroupB = new RadioGroup(this);
        vcPlanGroupB.setOrientation(RadioGroup.VERTICAL);

        String[] planLabels = {"5 클레임  (perf.claims.5)", "50 클레임  (perf.claims.50)", "100 클레임  (perf.claims.100)"};
        for (int i = 0; i < PERF_VC_PLAN_IDS.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setId(View.generateViewId());
            rb.setText(planLabels[i]);
            rb.setTextSize(14f);
            rb.setChecked(i == 0);
            final String planId = PERF_VC_PLAN_IDS[i];
            rb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) selectedVcPlanIdB = planId;
            });
            vcPlanGroupB.addView(rb);
        }
        vcPlanGroupB.setPadding(0, 4, 0, 24);
        root.addView(vcPlanGroupB);

        root.addView(buildStepperRow("Warmup (0-20)", warmupCount, WARMUP_MIN, WARMUP_MAX, true));
        root.addView(buildStepperRow("Measurement (1-50)", measurementCount, MEASUREMENT_MIN, MEASUREMENT_MAX, false));

        startButtonB = new Button(this);
        startButtonB.setText("테스트 시작");
        startButtonB.setOnClickListener(v -> startTestB());
        root.addView(startButtonB);

        View divider = new View(this);
        divider.setBackgroundColor(0xFFCCCCCC);
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2);
        divParams.setMargins(0, 32, 0, 16);
        root.addView(divider, divParams);

        scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        statusView = new TextView(this);
        statusView.setPadding(0, 8, 0, 0);
        statusView.setTextSize(12f);
        statusView.setTypeface(android.graphics.Typeface.MONOSPACE);
        scrollView.addView(statusView);
        root.addView(scrollView, scrollParams);

        return root;
    }

    // ────────────────────────────────────────────────
    // Scenario B 테스트 실행
    // ────────────────────────────────────────────────

    private void startTestB() {
        warmupCount = clamp(parseCount(warmupCountView, DEFAULT_WARMUP), WARMUP_MIN, WARMUP_MAX);
        measurementCount = clamp(parseCount(measurementCountView, DEFAULT_MEASUREMENT), MEASUREMENT_MIN, MEASUREMENT_MAX);

        selectedAlgorithm = classicButtonB.isChecked() ? "Secp256r1" : "MlDsa44";
        org.omnione.did.sdk.wallet.walletservice.config.Config.KEY_ALGORITHM = selectedAlgorithm;
        Log.i(TAG, "B: KEY_ALGORITHM set to: " + selectedAlgorithm);

        pqcButtonB.setEnabled(false);
        classicButtonB.setEnabled(false);
        for (int i = 0; i < vcPlanGroupB.getChildCount(); i++) vcPlanGroupB.getChildAt(i).setEnabled(false);
        startButtonB.setEnabled(false);

        bResults.clear();
        bConsecErrors = 0;
        setRunning(true);

        setStatus("Scenario B 시작\n알고리즘: " + selectedAlgorithm
                + "\nvcPlanId: " + selectedVcPlanIdB
                + "\nWarmup: " + warmupCount + "회 | Measurement: " + measurementCount + "회\n");
        Log.i(TAG, "=== Scenario B Start | algo=" + selectedAlgorithm
                + " | vcPlanId=" + selectedVcPlanIdB
                + " | warmup=" + warmupCount + " | measurement=" + measurementCount + " ===");

        new Thread(() -> {
            try {
                String firstName = "Test";
                String lastName = "UserPerf";
                if (Preference.getInit(AutoTestActivity.this)) {
                    appendStatus("  이미 등록된 사용자 — P132 건너뜀");
                    Log.i(TAG, "=== B: already registered, skipping P132 ===");
                    appendStatus("  사용자 정보 등록: " + lastName + firstName);
                    saveUserInfo(firstName, lastName);
                    WalletApi api = WalletApi.getInstance(AutoTestActivity.this);
                    String holderDid = api.getDIDDocument(Constants.DID_TYPE_HOLDER).getId();
                    registerHolderToIssuer(holderDid,
                            Preference.getPiiForDemo(AutoTestActivity.this),
                            "vc.schema." + selectedVcPlanIdB,
                            firstName, lastName);
                    appendStatus("  issuer holder 등록 완료 — P210 시작");
                    runScenarioBIteration(0, warmupCount > 0);
                } else {
                    appendStatus("  초기화 중...");
                    iterationReset();
                    appendStatus("  사용자 정보 등록: " + lastName + firstName);
                    saveUserInfo(firstName, lastName);
                    appendStatus("  P132 사용자 등록 (1회)...");
                    runScenarioBPreRegistration(firstName, lastName);
                }
            } catch (Exception e) {
                CaLog.e("Scenario B pre-registration error: " + e.getMessage());
                appendStatus("  [사전 등록 오류] " + e.getMessage());
                abortScenarioB();
            }
        }).start();
    }

    private void runScenarioBPreRegistration(String firstName, String lastName) {
        Map<String, String> reqData = new HashMap<>();
        reqData.put("pin", AUTO_PIN);

        UserRegistration reg = new UserRegistration(this, reqData);
        reg.setCallback(new BaseOperation.Callback() {
            @Override
            public void onSuccess(ProtocolData preData) {
                try {
                    WalletApi walletApi = WalletApi.getInstance(AutoTestActivity.this);
                    String hWalletToken = preData.gethWalletToken();

                    walletApi.generateKeyPair(hWalletToken, AUTO_PIN);
                    walletApi.createHolderDIDDoc(hWalletToken);

                    DIDDocument holderDoc = walletApi.getDIDDocument(Constants.DID_TYPE_HOLDER);
                    DIDDocument signedDoc = (DIDDocument) walletApi.addProofsToDocument(
                            holderDoc, List.of("pin"), holderDoc.getId(),
                            Constants.DID_TYPE_HOLDER, AUTO_PIN, false);
                    SignedDidDoc signedDidDoc = walletApi.createSignedDIDDoc(signedDoc);
                    reqData.put("signedDidDoc", signedDidDoc.toJson());

                    reg.setCallback(new BaseOperation.Callback() {
                        @Override
                        public void onSuccess(ProtocolData execData) {
                            String holderDid = "";
                            try {
                                WalletApi api = WalletApi.getInstance(AutoTestActivity.this);
                                api.saveDocument();
                                holderDid = api.getDIDDocument(Constants.DID_TYPE_HOLDER).getId();
                                Preference.setDID(AutoTestActivity.this, holderDid);
                                Preference.setInit(AutoTestActivity.this, true);
                            } catch (Exception e) {
                                CaLog.e("B: saveDocument error: " + e.getMessage());
                            }

                            try {
                                registerHolderToIssuer(holderDid,
                                        Preference.getPiiForDemo(AutoTestActivity.this),
                                        "vc.schema." + selectedVcPlanIdB,
                                        firstName, lastName);
                            } catch (Exception e) {
                                CaLog.e("B: registerHolderToIssuer error: " + e.getMessage());
                            }

                            appendStatus("  P132 완료 — P210 반복 시작");
                            Log.i(TAG, "=== B pre-registration done, starting P210 loop ===");
                            runScenarioBIteration(0, warmupCount > 0);
                        }

                        @Override
                        public void onFailure(int code, String msg) {
                            appendStatus("  [P132 execute 오류] " + code + ": " + msg);
                            Log.e(TAG, "B:PRE_REG execute FAIL " + code + " " + msg);
                            abortScenarioB();
                        }
                    }).execute();

                } catch (WalletException | WalletCoreException | UtilityException e) {
                    appendStatus("  [P132 키 생성 오류] " + e.getMessage());
                    Log.e(TAG, "B:PRE_REG key gen FAIL " + e.getMessage());
                    abortScenarioB();
                }
            }

            @Override
            public void onFailure(int code, String msg) {
                appendStatus("  [P132 preExecute 오류] " + code + ": " + msg);
                Log.e(TAG, "B:PRE_REG preExecute FAIL " + code + " " + msg);
                abortScenarioB();
            }
        }).preExecute();
    }

    private void runScenarioBIteration(int index, boolean isWarmup) {
        String tag = iterTag(index, isWarmup);
        appendStatus("\n" + tag + " 반복 시작");
        Log.i(TAG, tag + " B --- start ---");

        appendStatus("  P210 VC 발급...");
        long issueStart = System.nanoTime();
        runScenarioBIssuance(index, issueStart, isWarmup);
    }

    private void runScenarioBIssuance(int index, long issueStart, boolean isWarmup) {
        String tag = iterTag(index, isWarmup);
        Map<String, String> reqData = new HashMap<>();
        reqData.put("pin", AUTO_PIN);
        reqData.put("vcPlanId", selectedVcPlanIdB);

        VcIssuance vc = new VcIssuance(this, reqData);
        vc.setCallback(new BaseOperation.Callback() {
            @Override
            public void onSuccess(ProtocolData preData) {
                vc.setCallback(new BaseOperation.Callback() {
                    @Override
                    public void onSuccess(ProtocolData execData) {
                        long issueNs = System.nanoTime() - issueStart;
                        long serverNs = execData != null ? execData.getServerRoundTripNs() : 0L;
                        long appNs = Math.max(0L, issueNs - serverNs);
                        appendStatus("  P210 완료: total=" + msStr(issueNs)
                                + " | server=" + msStr(serverNs)
                                + " | app=" + msStr(appNs));
                        Log.i(TAG, tag + " B:ISSUE total=" + msStr(issueNs)
                                + " server=" + msStr(serverNs)
                                + " app=" + msStr(appNs));

                        bConsecErrors = 0;
                        if (!isWarmup) {
                            bResults.add(new long[]{issueNs, serverNs, appNs});
                        }

                        Log.i(TAG, tag + " B:ITER_DONE total=" + ns2ms(issueNs)
                                + "ms server=" + ns2ms(serverNs)
                                + "ms app=" + ns2ms(appNs) + "ms");
                        advanceIterationB(index, isWarmup);
                    }

                    @Override
                    public void onFailure(int code, String msg) {
                        appendStatus("  [P210 execute 오류] " + code + ": " + msg);
                        Log.e(TAG, tag + " B:ISSUE execute FAIL " + code + " " + msg);
                        bConsecErrors++;
                        if (bConsecErrors >= B_MAX_CONSECUTIVE_ERRORS) { abortScenarioB(); return; }
                        advanceIterationB(index, isWarmup);
                    }
                }).execute();
            }

            @Override
            public void onFailure(int code, String msg) {
                appendStatus("  [P210 preExecute 오류] " + code + ": " + msg);
                Log.e(TAG, tag + " B:ISSUE preExecute FAIL " + code + " " + msg);
                bConsecErrors++;
                if (bConsecErrors >= B_MAX_CONSECUTIVE_ERRORS) { abortScenarioB(); return; }
                advanceIterationB(index, isWarmup);
            }
        }).preExecute();
    }

    private void advanceIterationB(int index, boolean isWarmup) {
        int next = index + 1;
        if (isWarmup) {
            if (next < warmupCount) {
                runScenarioBIteration(next, true);
            } else {
                Log.i(TAG, "=== B WARMUP DONE, entering MEASUREMENT ===");
                appendStatus("\n[WARMUP DONE]");
                runScenarioBIteration(0, false);
            }
        } else {
            if (next < measurementCount) {
                runScenarioBIteration(next, false);
            } else {
                runOnUiThread(this::showScenarioBReport);
            }
        }
    }

    private void abortScenarioB() {
        Log.e(TAG, "=== Scenario B ABORT: consecutive errors=" + bConsecErrors + " ===");
        runOnUiThread(() -> {
            appendStatus("\n[중단] 연속 오류 " + bConsecErrors + "회 — 테스트를 중단합니다.");
            setRunning(false);
            if (startButtonB != null) {
                startButtonB.setEnabled(true);
                startButtonB.setText("다시 테스트");
                startButtonB.setOnClickListener(v -> showScenarioBConfig());
            }
        });
    }

    // ────────────────────────────────────────────────
    // Scenario B 리포트
    // ────────────────────────────────────────────────

    private void showScenarioBReport() {
        org.omnione.did.sdk.wallet.walletservice.config.Config.KEY_ALGORITHM = SDK_ALGO_DEFAULT;
        Log.i(TAG, "B: complete, KEY_ALGORITHM restored to " + SDK_ALGO_DEFAULT);
        setRunning(false);

        if (bResults.isEmpty()) {
            appendStatus("\n[리포트] 성공한 측정 반복이 없습니다.");
            Log.i(TAG, "=== B REPORT: no results ===");
            if (startButtonB != null) {
                startButtonB.setEnabled(true);
                startButtonB.setText("다시 테스트");
                startButtonB.setOnClickListener(v -> showScenarioBConfig());
            }
            return;
        }

        int n = bResults.size();
        long[] issueArr = new long[n];
        long[] serverArr = new long[n];
        long[] appArr = new long[n];

        for (int i = 0; i < n; i++) {
            issueArr[i] = bResults.get(i)[0];
            serverArr[i] = bResults.get(i)[1];
            appArr[i] = bResults.get(i)[2];
        }
        Arrays.sort(issueArr);
        Arrays.sort(serverArr);
        Arrays.sort(appArr);

        String report = String.format(
                "\n========== Scenario B Report ==========\n" +
                "알고리즘: %s\n" +
                "vcPlanId: %s\n" +
                "Warmup: %d회 (제외)\n" +
                "Measurement: %d회\n" +
                "(P132 사전 등록 1회, P210만 반복 측정)\n\n" +
                "[P210 전체]\n" +
                "mean  %s ms\n" +
                "p50   %s ms\n" +
                "p95   %s ms\n" +
                "min   %s ms\n" +
                "max   %s ms\n\n" +
                "[서버 API 왕복]\n" +
                "mean  %s ms\n" +
                "p50   %s ms\n" +
                "p95   %s ms\n" +
                "min   %s ms\n" +
                "max   %s ms\n\n" +
                "[앱 내부 처리]\n" +
                "mean  %s ms\n" +
                "p50   %s ms\n" +
                "p95   %s ms\n" +
                "min   %s ms\n" +
                "max   %s ms\n" +
                "=======================================",
                selectedAlgorithm, selectedVcPlanIdB, warmupCount, n,
                msFmt(mean(issueArr)), msFmt(calcP50(issueArr)), msFmt(calcP95(issueArr)),
                msFmt(issueArr[0]), msFmt(issueArr[n - 1]),
                msFmt(mean(serverArr)), msFmt(calcP50(serverArr)), msFmt(calcP95(serverArr)),
                msFmt(serverArr[0]), msFmt(serverArr[n - 1]),
                msFmt(mean(appArr)), msFmt(calcP50(appArr)), msFmt(calcP95(appArr)),
                msFmt(appArr[0]), msFmt(appArr[n - 1])
        );

        appendStatus(report);
        Log.i(TAG, report.replace("\n", " | "));
        Log.i(TAG, String.format(
                "B:SUMMARY algo=%s vcPlanId=%s n=%d warmup=%d "
                        + "meanIssue=%.2fms p95Issue=%.2fms "
                        + "meanServer=%.2fms p95Server=%.2fms "
                        + "meanApp=%.2fms p95App=%.2fms",
                selectedAlgorithm, selectedVcPlanIdB, n, warmupCount,
                ns2msD(mean(issueArr)), ns2msD(calcP95(issueArr)),
                ns2msD(mean(serverArr)), ns2msD(calcP95(serverArr)),
                ns2msD(mean(appArr)), ns2msD(calcP95(appArr))));

        java.util.LinkedHashMap<String, long[]> metrics = new java.util.LinkedHashMap<>();
        metrics.put("total",  issueArr);
        metrics.put("server", serverArr);
        metrics.put("app",    appArr);
        sendReportToObserver("B", selectedAlgorithm, selectedVcPlanIdB, null, n, metrics);

        if (startButtonB != null) {
            startButtonB.setEnabled(true);
            startButtonB.setText("다시 테스트");
            startButtonB.setOnClickListener(v -> showScenarioBConfig());
        }
    }

    // ────────────────────────────────────────────────
    // Scenario C 구성 화면
    // ────────────────────────────────────────────────

    private void showScenarioCConfig() {
        setActionBarTitle("Scenario C — VP 제출 클레임 수", true);
        cResults.clear();
        cConsecErrors = 0;
        isRunning = false;
        selectedVcPlanIdC  = PERF_VC_PLAN_IDS[0];
        selectedVpPolicyIdC = PERF_VP_POLICY_IDS[0];
        setContentView(buildScenarioCLayout());
    }

    private View buildScenarioCLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 64, 48, 32);

        TextView title = new TextView(this);
        title.setText("Scenario C — VP 제출 클레임 수 테스트");
        title.setTextSize(20f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 32);
        root.addView(title);

        TextView algoLabel = new TextView(this);
        algoLabel.setText("알고리즘 선택");
        algoLabel.setTextSize(14f);
        algoLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        algoLabel.setPadding(0, 0, 0, 8);
        root.addView(algoLabel);

        algorithmGroupC = new RadioGroup(this);
        algorithmGroupC.setOrientation(RadioGroup.VERTICAL);

        pqcButtonC = new RadioButton(this);
        pqcButtonC.setId(View.generateViewId());
        pqcButtonC.setText("PQC  (ML-DSA-44)");
        pqcButtonC.setChecked(true);
        pqcButtonC.setTextSize(14f);

        classicButtonC = new RadioButton(this);
        classicButtonC.setId(View.generateViewId());
        classicButtonC.setText("Classic  (Secp256r1)");
        classicButtonC.setTextSize(14f);

        algorithmGroupC.addView(pqcButtonC);
        algorithmGroupC.addView(classicButtonC);
        algorithmGroupC.setPadding(0, 4, 0, 24);
        root.addView(algorithmGroupC);

        TextView planLabel = new TextView(this);
        planLabel.setText("VP 클레임 수 선택");
        planLabel.setTextSize(14f);
        planLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        planLabel.setPadding(0, 0, 0, 8);
        root.addView(planLabel);

        vpPlanGroupC = new RadioGroup(this);
        vpPlanGroupC.setOrientation(RadioGroup.VERTICAL);

        String[] planLabels = {"5 클레임  (perf.claims.5)", "50 클레임  (perf.claims.50)", "100 클레임  (perf.claims.100)"};
        for (int i = 0; i < PERF_VC_PLAN_IDS.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setId(View.generateViewId());
            rb.setText(planLabels[i]);
            rb.setTextSize(14f);
            rb.setChecked(i == 0);
            final int idx = i;
            rb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedVcPlanIdC   = PERF_VC_PLAN_IDS[idx];
                    selectedVpPolicyIdC = PERF_VP_POLICY_IDS[idx];
                }
            });
            vpPlanGroupC.addView(rb);
        }
        vpPlanGroupC.setPadding(0, 4, 0, 24);
        root.addView(vpPlanGroupC);

        root.addView(buildStepperRow("Warmup (0-20)", warmupCount, WARMUP_MIN, WARMUP_MAX, true));
        root.addView(buildStepperRow("Measurement (1-50)", measurementCount, MEASUREMENT_MIN, MEASUREMENT_MAX, false));

        startButtonC = new Button(this);
        startButtonC.setText("테스트 시작");
        startButtonC.setOnClickListener(v -> startTestC());
        root.addView(startButtonC);

        View divider = new View(this);
        divider.setBackgroundColor(0xFFCCCCCC);
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2);
        divParams.setMargins(0, 32, 0, 16);
        root.addView(divider, divParams);

        scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        statusView = new TextView(this);
        statusView.setPadding(0, 8, 0, 0);
        statusView.setTextSize(12f);
        statusView.setTypeface(android.graphics.Typeface.MONOSPACE);
        scrollView.addView(statusView);
        root.addView(scrollView, scrollParams);

        return root;
    }

    // ────────────────────────────────────────────────
    // Scenario C 테스트 실행
    // ────────────────────────────────────────────────

    private void startTestC() {
        warmupCount = clamp(parseCount(warmupCountView, DEFAULT_WARMUP), WARMUP_MIN, WARMUP_MAX);
        measurementCount = clamp(parseCount(measurementCountView, DEFAULT_MEASUREMENT), MEASUREMENT_MIN, MEASUREMENT_MAX);

        String algo = classicButtonC.isChecked() ? "Secp256r1" : "MlDsa44";
        selectedAlgorithm = algo;
        org.omnione.did.sdk.wallet.walletservice.config.Config.KEY_ALGORITHM = algo;
        Log.i(TAG_C, "C: KEY_ALGORITHM set to: " + algo);

        pqcButtonC.setEnabled(false);
        classicButtonC.setEnabled(false);
        for (int i = 0; i < vpPlanGroupC.getChildCount(); i++) vpPlanGroupC.getChildAt(i).setEnabled(false);
        startButtonC.setEnabled(false);

        cResults.clear();
        cConsecErrors = 0;
        setRunning(true);

        setStatus("Scenario C 시작\n알고리즘: " + algo
                + "\nvcPlanId: " + selectedVcPlanIdC
                + "\npolicyId: " + selectedVpPolicyIdC
                + "\nWarmup: " + warmupCount + "회 | Measurement: " + measurementCount + "회"
                + "\n(P132 + P210 사전 준비 1회, P310만 반복 측정)\n");
        Log.i(TAG_C, "=== Scenario C Start | algo=" + algo
                + " | vcPlanId=" + selectedVcPlanIdC
                + " | policyId=" + selectedVpPolicyIdC
                + " | warmup=" + warmupCount + " | measurement=" + measurementCount + " ===");

        new Thread(this::runScenarioCPreparation).start();
    }

    private void runScenarioCPreparation() {
        appendStatus("\n[준비] 사용자 등록 및 VC 발급 1회 수행");
        Log.i(TAG_C, "=== C PREPARE START ===");
        try {
            String firstName = "Test";
            String lastName = "User001";
            if (Preference.getInit(AutoTestActivity.this)) {
                appendStatus("  이미 등록된 사용자 — P132 건너뜀");
                Log.i(TAG_C, "=== C: already registered, skipping P132 ===");
                appendStatus("  사용자 정보 등록: " + lastName + firstName);
                saveUserInfo(firstName, lastName);
                WalletApi api = WalletApi.getInstance(AutoTestActivity.this);
                String holderDid = api.getDIDDocument(Constants.DID_TYPE_HOLDER).getId();
                registerHolderToIssuer(holderDid,
                        Preference.getPiiForDemo(AutoTestActivity.this),
                        "vc.schema." + selectedVcPlanIdC,
                        firstName, lastName);
                appendStatus("  issuer holder 등록 완료 — P210 VC 발급...");
                long issueStart = System.nanoTime();
                runScenarioCIssuance(issueStart);
            } else {
                appendStatus("  초기화 중...");
                iterationReset();
                appendStatus("  사용자 정보 등록: " + lastName + firstName);
                saveUserInfo(firstName, lastName);
                appendStatus("  P132 사용자 등록...");
                long regStart = System.nanoTime();
                runScenarioCRegistration(regStart, firstName, lastName);
            }
        } catch (Exception e) {
            CaLog.e("Scenario C preparation error: " + e.getMessage());
            appendStatus("  [오류] " + e.getMessage());
            abortScenarioC();
        }
    }

    private void runScenarioCIteration(int index, boolean isWarmup) {
        String tag = iterTag(index, isWarmup);
        appendStatus("\n" + tag + " 반복 시작");
        Log.i(TAG_C, tag + " C --- start ---");

        appendStatus("  P310 VP 제출...");
        long vpStart = System.nanoTime();
        try {
            TimedOfferResult offerResult = fetchOfferQrTimed(selectedVpPolicyIdC);
            runScenarioCVp(index, vpStart, offerResult.offerId, offerResult.serverNs, isWarmup);
        } catch (Exception e) {
            appendStatus("  [VP offer 오류] " + e.getMessage());
            Log.e(TAG_C, tag + " C:VP offer FAIL " + e.getMessage());
            cConsecErrors++;
            if (cConsecErrors >= C_MAX_CONSECUTIVE_ERRORS) { abortScenarioC(); return; }
            advanceIterationC(index, isWarmup);
        }
    }

    private void runScenarioCRegistration(long regStart, String firstName, String lastName) {
        Map<String, String> reqData = new HashMap<>();
        reqData.put("pin", AUTO_PIN);

        UserRegistration reg = new UserRegistration(this, reqData);
        reg.setCallback(new BaseOperation.Callback() {
            @Override
            public void onSuccess(ProtocolData preData) {
                try {
                    WalletApi walletApi = WalletApi.getInstance(AutoTestActivity.this);
                    String hWalletToken = preData.gethWalletToken();

                    walletApi.generateKeyPair(hWalletToken, AUTO_PIN);
                    walletApi.createHolderDIDDoc(hWalletToken);

                    DIDDocument holderDoc = walletApi.getDIDDocument(Constants.DID_TYPE_HOLDER);
                    DIDDocument signedDoc = (DIDDocument) walletApi.addProofsToDocument(
                            holderDoc, List.of("pin"), holderDoc.getId(),
                            Constants.DID_TYPE_HOLDER, AUTO_PIN, false);
                    SignedDidDoc signedDidDoc = walletApi.createSignedDIDDoc(signedDoc);
                    reqData.put("signedDidDoc", signedDidDoc.toJson());

                    reg.setCallback(new BaseOperation.Callback() {
                        @Override
                        public void onSuccess(ProtocolData execData) {
                            String holderDid = "";
                            try {
                                WalletApi api = WalletApi.getInstance(AutoTestActivity.this);
                                api.saveDocument();
                                holderDid = api.getDIDDocument(Constants.DID_TYPE_HOLDER).getId();
                                Preference.setDID(AutoTestActivity.this, holderDid);
                                Preference.setInit(AutoTestActivity.this, true);
                            } catch (Exception e) {
                                CaLog.e("C: saveDocument error: " + e.getMessage());
                            }

                            try {
                                registerHolderToIssuer(holderDid,
                                        Preference.getPiiForDemo(AutoTestActivity.this),
                                        "vc.schema." + selectedVcPlanIdC,
                                        firstName, lastName);
                            } catch (Exception e) {
                                CaLog.e("C: registerHolderToIssuer error: " + e.getMessage());
                            }

                            long regNs = System.nanoTime() - regStart;
                            appendStatus("  P132 완료: " + msStr(regNs));
                            Log.i(TAG_C, "C:PREP REG " + msStr(regNs));

                            appendStatus("  P210 VC 발급...");
                            long issueStart = System.nanoTime();
                            runScenarioCIssuance(issueStart);
                        }

                        @Override
                        public void onFailure(int code, String msg) {
                            appendStatus("  [P132 execute 오류] " + code + ": " + msg);
                            Log.e(TAG_C, "C:PREP REG execute FAIL " + code + " " + msg);
                            abortScenarioC();
                        }
                    }).execute();

                } catch (WalletException | WalletCoreException | UtilityException e) {
                    appendStatus("  [P132 키 생성 오류] " + e.getMessage());
                    Log.e(TAG_C, "C:PREP REG key gen FAIL " + e.getMessage());
                    abortScenarioC();
                }
            }

            @Override
            public void onFailure(int code, String msg) {
                appendStatus("  [P132 preExecute 오류] " + code + ": " + msg);
                Log.e(TAG_C, "C:PREP REG preExecute FAIL " + code + " " + msg);
                abortScenarioC();
            }
        }).preExecute();
    }

    private void runScenarioCIssuance(long issueStart) {
        Map<String, String> reqData = new HashMap<>();
        reqData.put("pin", AUTO_PIN);
        reqData.put("vcPlanId", selectedVcPlanIdC);

        VcIssuance vc = new VcIssuance(this, reqData);
        vc.setCallback(new BaseOperation.Callback() {
            @Override
            public void onSuccess(ProtocolData preData) {
                vc.setCallback(new BaseOperation.Callback() {
                    @Override
                    public void onSuccess(ProtocolData execData) {
                        long issueNs = System.nanoTime() - issueStart;
                        appendStatus("  P210 완료: " + msStr(issueNs));
                        Log.i(TAG_C, "C:PREP ISSUE " + msStr(issueNs));

                        appendStatus("\n[준비 완료] VP 제출 반복 측정을 시작합니다.");
                        cConsecErrors = 0;
                        if (warmupCount > 0) {
                            runScenarioCIteration(0, true);
                        } else {
                            runScenarioCIteration(0, false);
                        }
                    }

                    @Override
                    public void onFailure(int code, String msg) {
                        appendStatus("  [P210 execute 오류] " + code + ": " + msg);
                        Log.e(TAG_C, "C:PREP ISSUE execute FAIL " + code + " " + msg);
                        abortScenarioC();
                    }
                }).execute();
            }

            @Override
            public void onFailure(int code, String msg) {
                appendStatus("  [P210 preExecute 오류] " + code + ": " + msg);
                Log.e(TAG_C, "C:PREP ISSUE preExecute FAIL " + code + " " + msg);
                abortScenarioC();
            }
        }).preExecute();
    }

    private void runScenarioCVp(int index, long vpStart, String offerId, long offerServerNs,
                                boolean isWarmup) {
        String tag = iterTag(index, isWarmup);
        Map<String, String> reqData = new HashMap<>();
        reqData.put("pin", AUTO_PIN);
        reqData.put("offerId", offerId);
        reqData.put("payloadType", "Direct");

        VpSubmission vp = new VpSubmission(this, reqData);
        vp.setCallback(new BaseOperation.Callback() {
            @Override
            public void onSuccess(ProtocolData preData) {
                vp.setCallback(new BaseOperation.Callback() {
                    @Override
                    public void onSuccess(ProtocolData execData) {
                        long vpNs = System.nanoTime() - vpStart;
                        long protocolServerNs = execData != null ? execData.getServerRoundTripNs() : 0L;
                        long serverNs = offerServerNs + protocolServerNs;
                        long appNs = Math.max(0L, vpNs - serverNs);
                        appendStatus("  P310 완료: total=" + msStr(vpNs)
                                + " | server=" + msStr(serverNs)
                                + " | app=" + msStr(appNs));
                        Log.i(TAG_C, tag + " C:VP total=" + msStr(vpNs)
                                + " server=" + msStr(serverNs)
                                + " app=" + msStr(appNs));

                        cConsecErrors = 0;
                        if (!isWarmup) {
                            cResults.add(new long[]{vpNs, serverNs, appNs});
                        }

                        Log.i(TAG_C, tag + " C:ITER_DONE"
                                + " total=" + ns2ms(vpNs) + "ms"
                                + " server=" + ns2ms(serverNs) + "ms"
                                + " app=" + ns2ms(appNs) + "ms");

                        advanceIterationC(index, isWarmup);
                    }

                    @Override
                    public void onFailure(int code, String msg) {
                        appendStatus("  [P310 execute 오류] " + code + ": " + msg);
                        Log.e(TAG_C, tag + " C:VP execute FAIL " + code + " " + msg);
                        cConsecErrors++;
                        if (cConsecErrors >= C_MAX_CONSECUTIVE_ERRORS) { abortScenarioC(); return; }
                        advanceIterationC(index, isWarmup);
                    }
                }).execute();
            }

            @Override
            public void onFailure(int code, String msg) {
                appendStatus("  [P310 preExecute 오류] " + code + ": " + msg);
                Log.e(TAG_C, tag + " C:VP preExecute FAIL " + code + " " + msg);
                cConsecErrors++;
                if (cConsecErrors >= C_MAX_CONSECUTIVE_ERRORS) { abortScenarioC(); return; }
                advanceIterationC(index, isWarmup);
            }
        }).preExecute();
    }

    private void advanceIterationC(int index, boolean isWarmup) {
        int next = index + 1;
        if (isWarmup) {
            if (next < warmupCount) {
                runScenarioCIteration(next, true);
            } else {
                Log.i(TAG_C, "=== C WARMUP DONE, entering MEASUREMENT ===");
                appendStatus("\n[WARMUP DONE]");
                runScenarioCIteration(0, false);
            }
        } else {
            if (next < measurementCount) {
                runScenarioCIteration(next, false);
            } else {
                runOnUiThread(this::showScenarioCReport);
            }
        }
    }

    private void abortScenarioC() {
        Log.e(TAG_C, "=== Scenario C ABORT: consecutive errors=" + cConsecErrors + " ===");
        runOnUiThread(() -> {
            appendStatus("\n[중단] 연속 오류 " + cConsecErrors + "회 — 테스트를 중단합니다.");
            setRunning(false);
            org.omnione.did.sdk.wallet.walletservice.config.Config.KEY_ALGORITHM = SDK_ALGO_DEFAULT;
            if (startButtonC != null) {
                startButtonC.setEnabled(true);
                startButtonC.setText("다시 테스트");
                startButtonC.setOnClickListener(v -> showScenarioCConfig());
            }
        });
    }

    // ────────────────────────────────────────────────
    // Scenario C 리포트
    // ────────────────────────────────────────────────

    private void showScenarioCReport() {
        org.omnione.did.sdk.wallet.walletservice.config.Config.KEY_ALGORITHM = SDK_ALGO_DEFAULT;
        Log.i(TAG_C, "C: complete, KEY_ALGORITHM restored to " + SDK_ALGO_DEFAULT);
        setRunning(false);

        if (cResults.isEmpty()) {
            appendStatus("\n[리포트] 성공한 측정 반복이 없습니다.");
            Log.i(TAG_C, "=== C REPORT: no results ===");
            if (startButtonC != null) {
                startButtonC.setEnabled(true);
                startButtonC.setText("다시 테스트");
                startButtonC.setOnClickListener(v -> showScenarioCConfig());
            }
            return;
        }

        int n = cResults.size();
        long[] totalArr = new long[n];
        long[] serverArr = new long[n];
        long[] appArr = new long[n];

        for (int i = 0; i < n; i++) {
            long[] r = cResults.get(i);
            totalArr[i] = r[0];
            serverArr[i] = r[1];
            appArr[i] = r[2];
        }
        Arrays.sort(totalArr);
        Arrays.sort(serverArr);
        Arrays.sort(appArr);

        String report = String.format(
                "\n========== Scenario C Report ==========\n" +
                "알고리즘: %s\n" +
                "vcPlanId: %s\n" +
                "policyId: %s\n" +
                "Warmup: %d회 (제외)\n" +
                "Measurement: %d회\n" +
                "(P132 + P210 사전 준비 1회, P310만 반복 측정)\n\n" +
                "[P310 전체]\n" +
                "mean  %s ms\n" +
                "p50   %s ms\n" +
                "p95   %s ms\n" +
                "min   %s ms\n" +
                "max   %s ms\n\n" +
                "[서버 API 왕복]\n" +
                "mean  %s ms\n" +
                "p50   %s ms\n" +
                "p95   %s ms\n" +
                "min   %s ms\n" +
                "max   %s ms\n\n" +
                "[앱 내부 처리]\n" +
                "mean  %s ms\n" +
                "p50   %s ms\n" +
                "p95   %s ms\n" +
                "min   %s ms\n" +
                "max   %s ms\n" +
                "=======================================",
                selectedAlgorithm, selectedVcPlanIdC, selectedVpPolicyIdC, warmupCount, n,
                msFmt(mean(totalArr)), msFmt(calcP50(totalArr)), msFmt(calcP95(totalArr)),
                msFmt(totalArr[0]), msFmt(totalArr[n - 1]),
                msFmt(mean(serverArr)), msFmt(calcP50(serverArr)), msFmt(calcP95(serverArr)),
                msFmt(serverArr[0]), msFmt(serverArr[n - 1]),
                msFmt(mean(appArr)), msFmt(calcP50(appArr)), msFmt(calcP95(appArr)),
                msFmt(appArr[0]), msFmt(appArr[n - 1])
        );

        appendStatus(report);
        Log.i(TAG_C, report.replace("\n", " | "));
        Log.i(TAG_C, String.format(
                "C:SUMMARY algo=%s vcPlanId=%s policyId=%s n=%d warmup=%d "
                        + "meanVp=%.2fms p95Vp=%.2fms "
                        + "meanServer=%.2fms p95Server=%.2fms "
                        + "meanApp=%.2fms p95App=%.2fms",
                selectedAlgorithm, selectedVcPlanIdC, selectedVpPolicyIdC, n, warmupCount,
                ns2msD(mean(totalArr)), ns2msD(calcP95(totalArr)),
                ns2msD(mean(serverArr)), ns2msD(calcP95(serverArr)),
                ns2msD(mean(appArr)), ns2msD(calcP95(appArr))));

        java.util.LinkedHashMap<String, long[]> metrics = new java.util.LinkedHashMap<>();
        metrics.put("total",  totalArr);
        metrics.put("server", serverArr);
        metrics.put("app",    appArr);
        sendReportToObserver("C", selectedAlgorithm, selectedVcPlanIdC, selectedVpPolicyIdC, n, metrics);

        if (startButtonC != null) {
            startButtonC.setEnabled(true);
            startButtonC.setText("다시 테스트");
            startButtonC.setOnClickListener(v -> showScenarioCConfig());
        }
    }

    // ────────────────────────────────────────────────
    // 테스트 시작
    // ────────────────────────────────────────────────

    private void startTest() {
        warmupCount = clamp(parseCount(warmupCountView, DEFAULT_WARMUP), WARMUP_MIN, WARMUP_MAX);
        measurementCount = clamp(parseCount(measurementCountView, DEFAULT_MEASUREMENT),
                MEASUREMENT_MIN, MEASUREMENT_MAX);

        selectedAlgorithm = classicButton.isChecked() ? "Secp256r1" : "MlDsa44";
        org.omnione.did.sdk.wallet.walletservice.config.Config.KEY_ALGORITHM = selectedAlgorithm;
        Log.i(TAG, "KEY_ALGORITHM set to: " + selectedAlgorithm);

        algorithmGroup.setEnabled(false);
        pqcButton.setEnabled(false);
        classicButton.setEnabled(false);
        for (int i = 0; i < vpPlanGroupA.getChildCount(); i++) vpPlanGroupA.getChildAt(i).setEnabled(false);
        startButton.setEnabled(false);

        results.clear();
        setRunning(true);

        setStatus("자동 테스트 시작\n알고리즘: " + selectedAlgorithm
                + "\nvcPlanId: " + selectedVcPlanIdA
                + "\nWarmup: " + warmupCount + "회 | Measurement: " + measurementCount + "회"
                + "\npolicyId: " + selectedVpPolicyIdA + "\n");
        Log.i(TAG, "=== PQC Auto Test Start | algo=" + selectedAlgorithm
                + " | vcPlanId=" + selectedVcPlanIdA
                + " | warmup=" + warmupCount + " | measurement=" + measurementCount
                + " | policyId=" + selectedVpPolicyIdA + " ===");

        boolean startWithWarmup = warmupCount > 0;
        new Thread(() -> runIteration(0, startWithWarmup)).start();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // ────────────────────────────────────────────────
    // 메인 루프
    // ────────────────────────────────────────────────

    private String iterTag(int index, boolean isWarmup) {
        if (isWarmup) {
            return "[WARMUP " + (index + 1) + "/" + warmupCount + "]";
        }
        return "[MEASURE " + (index + 1) + "/" + measurementCount + "]";
    }

    private void runIteration(int index, boolean isWarmup) {
        String tag = iterTag(index, isWarmup);
        appendStatus("\n" + tag + " 반복 시작");
        Log.i(TAG, tag + " --- start ---");

        try {
            appendStatus("  초기화 중...");
            iterationReset();

            String firstName = "Test";
            String lastName = String.format("User%03d", index + 1);
            appendStatus("  사용자 정보 등록: " + lastName + firstName);
            saveUserInfo(firstName, lastName);

            appendStatus("  P132 사용자 등록...");
            long regStart = System.nanoTime();
            runRegistration(index, regStart, isWarmup, firstName, lastName);

        } catch (Exception e) {
            CaLog.e("AutoTest iteration " + index + " error: " + e.getMessage());
            appendStatus("  [오류] " + e.getMessage());
            aConsecErrors++;
            if (aConsecErrors >= A_MAX_CONSECUTIVE_ERRORS) { abortScenarioA(); return; }
            advanceIteration(index, isWarmup);
        }
    }

    private void advanceIteration(int index, boolean isWarmup) {
        int next = index + 1;
        if (isWarmup) {
            if (next < warmupCount) {
                runIteration(next, true);
            } else {
                // Warmup 완료 → 측정 단계 진입
                Log.i(TAG, "=== WARMUP DONE, entering MEASUREMENT ===");
                runIteration(0, false);
            }
        } else {
            if (next < measurementCount) {
                runIteration(next, false);
            } else {
                printReport();
            }
        }
    }

    // ────────────────────────────────────────────────
    // Iteration Reset
    // ────────────────────────────────────────────────

    private void iterationReset() throws WalletCoreException, WalletException, UtilityException, ExecutionException, InterruptedException {
        ProtocolData.getInstance().resetData();
        Preference.resetForIteration(this);

        WalletApi walletApi = WalletApi.getInstance(this);
        // deleteWallet(true): device key/DID까지 완전 삭제 → createWallet 시 항상 clean state로 재생성
        // (false 사용 시 device DID 파일이 saveDocument 이후 null 상태로 남아 requestECDH에서 not-found 발생)
        if (walletApi.isExistWallet()) {
            walletApi.deleteWallet(true);
        }
        walletApi.createWallet(Config.WAS.BASE_URL, Config.TAS.BASE_URL);
        // createDeviceDocument() 끝에서 isLock=true로 설정되므로, DB 상태 기준으로 다시 동기화
        walletApi.isLock();
    }

    // ────────────────────────────────────────────────
    // Demo server: save-user-info
    // ────────────────────────────────────────────────

    private void saveUserInfo(String firstName, String lastName) throws Exception {
        JSONObject body = new JSONObject();
        body.put("firstname", firstName);
        body.put("lastname", lastName);

        String result = new HttpUrlConnection().send(this, Config.Base_Demo.SAVE_USER_INFO, "POST", body.toString());

        JSONObject response = new JSONObject(result);
        String userId = response.getString("userId");
        String pii = response.optString("pii", "");
        String username = response.optString("username", lastName + firstName);

        Preference.setUserIdForDemo(this, userId);
        Preference.setPiiForDemo(this, pii);
        Preference.setUsernameForDemo(this, username);

        CaLog.d("saveUserInfo OK | userId=" + userId);
    }

    // ────────────────────────────────────────────────
    // Demo server: register-holder-to-issuer (P132 완료 후)
    // ────────────────────────────────────────────────

    private void registerHolderToIssuer(String holderDid, String pii, String vcSchemaId,
                                         String firstName, String lastName) throws Exception {
        JSONObject userInfo = new JSONObject();
        userInfo.put("pii", pii);

        if (vcSchemaId.startsWith("vc.schema.perf.claims.")) {
            String ns = vcSchemaId.substring("vc.schema.".length()); // e.g. "perf.claims.5"
            int count = Integer.parseInt(ns.substring(ns.lastIndexOf('.') + 1));
            for (int i = 1; i <= count; i++) {
                userInfo.put(String.format("%s.field%02d", ns, i), String.format("value%02d", i));
            }
        } else {
            userInfo.put("person.basic.name", firstName + " " + lastName);
            userInfo.put("person.basic.birth", "19900101");
            userInfo.put("person.basic.gender", "M");
            for (int i = 4; i <= 30; i++) {
                userInfo.put(String.format("person.basic.field%02d", i), String.format("value%02d", i));
            }
        }

        JSONObject body = new JSONObject();
        body.put("did", holderDid);
        body.put("pii", pii);
        body.put("vcSchemaId", vcSchemaId);
        body.put("userInfo", userInfo.toString());

        new HttpUrlConnection().send(this, Config.Base_Demo.REGISTER_HOLDER_TO_ISSUER, "POST", body.toString());
        CaLog.d("registerHolderToIssuer OK | did=" + holderDid + " vcSchemaId=" + vcSchemaId);
    }

    // ────────────────────────────────────────────────
    // P132 사용자 등록
    // ────────────────────────────────────────────────

    private void runRegistration(int index, long regStart, boolean isWarmup,
                                  String firstName, String lastName) {
        String tag = iterTag(index, isWarmup);
        Map<String, String> reqData = new HashMap<>();
        reqData.put("pin", AUTO_PIN);

        UserRegistration reg = new UserRegistration(this, reqData);
        reg.setCallback(new BaseOperation.Callback() {
            @Override
            public void onSuccess(ProtocolData preData) {
                try {
                    WalletApi walletApi = WalletApi.getInstance(AutoTestActivity.this);
                    String hWalletToken = preData.gethWalletToken();

                    walletApi.generateKeyPair(hWalletToken, AUTO_PIN);
                    walletApi.createHolderDIDDoc(hWalletToken);

                    DIDDocument holderDoc = walletApi.getDIDDocument(Constants.DID_TYPE_HOLDER);
                    DIDDocument signedDoc = (DIDDocument) walletApi.addProofsToDocument(
                            holderDoc, List.of("pin"), holderDoc.getId(),
                            Constants.DID_TYPE_HOLDER, AUTO_PIN, false);
                    SignedDidDoc signedDidDoc = walletApi.createSignedDIDDoc(signedDoc);
                    reqData.put("signedDidDoc", signedDidDoc.toJson());

                    reg.setCallback(new BaseOperation.Callback() {
                        @Override
                        public void onSuccess(ProtocolData execData) {
                            String holderDid = "";
                            try {
                                WalletApi api = WalletApi.getInstance(AutoTestActivity.this);
                                api.saveDocument();
                                holderDid = api.getDIDDocument(Constants.DID_TYPE_HOLDER).getId();
                                Preference.setDID(AutoTestActivity.this, holderDid);
                                Preference.setInit(AutoTestActivity.this, true);
                            } catch (Exception e) {
                                CaLog.e("saveDocument error: " + e.getMessage());
                            }

                            // user_init 플로우: Issuer user 테이블에 holder DID 등록
                            try {
                                registerHolderToIssuer(holderDid,
                                        Preference.getPiiForDemo(AutoTestActivity.this),
                                        buildVcSchemaId(selectedVcPlanIdA),
                                        firstName, lastName);
                            } catch (Exception e) {
                                CaLog.e("registerHolderToIssuer error: " + e.getMessage());
                            }

                            long regNs = System.nanoTime() - regStart;
                            appendStatus("  P132 완료: " + msStr(regNs));
                            Log.i(TAG, tag + " REG " + msStr(regNs));

                            appendStatus("  P210 VC 발급...");
                            long issueStart = System.nanoTime();
                            runVcIssuance(index, regNs, issueStart, isWarmup, selectedVcPlanIdA);
                        }

                        @Override
                        public void onFailure(int code, String msg) {
                            appendStatus("  [P132 execute 오류] " + code + ": " + msg);
                            Log.e(TAG, tag + " REG execute FAIL " + code + " " + msg);
                            aConsecErrors++;
                            if (aConsecErrors >= A_MAX_CONSECUTIVE_ERRORS) { abortScenarioA(); return; }
                            advanceIteration(index, isWarmup);
                        }
                    }).execute();

                } catch (WalletException | WalletCoreException | UtilityException e) {
                    appendStatus("  [P132 키 생성 오류] " + e.getMessage());
                    Log.e(TAG, tag + " REG key gen FAIL " + e.getMessage());
                    aConsecErrors++;
                    if (aConsecErrors >= A_MAX_CONSECUTIVE_ERRORS) { abortScenarioA(); return; }
                    advanceIteration(index, isWarmup);
                }
            }

            @Override
            public void onFailure(int code, String msg) {
                appendStatus("  [P132 preExecute 오류] " + code + ": " + msg);
                Log.e(TAG, tag + " REG preExecute FAIL " + code + " " + msg);
                aConsecErrors++;
                if (aConsecErrors >= A_MAX_CONSECUTIVE_ERRORS) { abortScenarioA(); return; }
                advanceIteration(index, isWarmup);
            }
        }).preExecute();
    }

    // ────────────────────────────────────────────────
    // P210 VC 발급
    // ────────────────────────────────────────────────

    private void runVcIssuance(int index, long regNs, long issueStart, boolean isWarmup, String vcPlanId) {
        String tag = iterTag(index, isWarmup);
        Map<String, String> reqData = new HashMap<>();
        reqData.put("pin", AUTO_PIN);
        reqData.put("vcPlanId", vcPlanId);

        VcIssuance vc = new VcIssuance(this, reqData);
        vc.setCallback(new BaseOperation.Callback() {
            @Override
            public void onSuccess(ProtocolData preData) {
                vc.setCallback(new BaseOperation.Callback() {
                    @Override
                    public void onSuccess(ProtocolData execData) {
                        long issueNs = System.nanoTime() - issueStart;
                        appendStatus("  P210 완료: " + msStr(issueNs));
                        Log.i(TAG, tag + " ISSUE " + msStr(issueNs));

                        appendStatus("  P310 VP 제출...");
                        long vpStart = System.nanoTime();
                        try {
                            String offerId = fetchOfferQr();
                            runVpSubmission(index, regNs, issueNs, vpStart, offerId, isWarmup);
                        } catch (Exception e) {
                            appendStatus("  [VP offer 오류] " + e.getMessage());
                            Log.e(TAG, tag + " VP offer FAIL " + e.getMessage());
                            aConsecErrors++;
                            if (aConsecErrors >= A_MAX_CONSECUTIVE_ERRORS) { abortScenarioA(); return; }
                            advanceIteration(index, isWarmup);
                        }
                    }

                    @Override
                    public void onFailure(int code, String msg) {
                        appendStatus("  [P210 execute 오류] " + code + ": " + msg);
                        Log.e(TAG, tag + " ISSUE execute FAIL " + code + " " + msg);
                        aConsecErrors++;
                        if (aConsecErrors >= A_MAX_CONSECUTIVE_ERRORS) { abortScenarioA(); return; }
                        advanceIteration(index, isWarmup);
                    }
                }).execute();
            }

            @Override
            public void onFailure(int code, String msg) {
                appendStatus("  [P210 preExecute 오류] " + code + ": " + msg);
                Log.e(TAG, tag + " ISSUE preExecute FAIL " + code + " " + msg);
                aConsecErrors++;
                if (aConsecErrors >= A_MAX_CONSECUTIVE_ERRORS) { abortScenarioA(); return; }
                advanceIteration(index, isWarmup);
            }
        }).preExecute();
    }

    // ────────────────────────────────────────────────
    // Verifier offer API 직접 호출
    // ────────────────────────────────────────────────

    private String fetchOfferQr() throws Exception {
        return fetchOfferQr(selectedVpPolicyIdA);
    }

    private TimedOfferResult fetchOfferQrTimed(String pid) throws Exception {
        JSONObject body = new JSONObject();
        body.put("policyId", pid);

        long startNs = System.nanoTime();
        String result = new HttpUrlConnection().send(this, Config.Verifier.REQUEST_OFFER_QR, "POST", body.toString());
        long serverNs = System.nanoTime() - startNs;
        CaLog.d("fetchOfferQr response: " + result);

        JSONObject response = new JSONObject(result);
        String offerId;
        if (response.has("payload")) {
            Object payload = response.get("payload");
            if (payload instanceof JSONObject) {
                offerId = ((JSONObject) payload).getString("offerId");
            } else {
                JSONObject payloadObj = new JSONObject(payload.toString());
                offerId = payloadObj.getString("offerId");
            }
        } else {
            offerId = response.getString("offerId");
        }
        return new TimedOfferResult(offerId, serverNs);
    }

    private String fetchOfferQr(String pid) throws Exception {
        JSONObject body = new JSONObject();
        body.put("policyId", pid);

        String result = new HttpUrlConnection().send(this, Config.Verifier.REQUEST_OFFER_QR, "POST", body.toString());
        CaLog.d("fetchOfferQr response: " + result);

        JSONObject response = new JSONObject(result);

        if (response.has("payload")) {
            Object payload = response.get("payload");
            if (payload instanceof JSONObject) {
                return ((JSONObject) payload).getString("offerId");
            } else {
                JSONObject payloadObj = new JSONObject(payload.toString());
                return payloadObj.getString("offerId");
            }
        }
        return response.getString("offerId");
    }

    private static final class TimedOfferResult {
        private final String offerId;
        private final long serverNs;

        private TimedOfferResult(String offerId, long serverNs) {
            this.offerId = offerId;
            this.serverNs = serverNs;
        }
    }

    // ────────────────────────────────────────────────
    // P310 VP 제출
    // ────────────────────────────────────────────────

    private void runVpSubmission(int index, long regNs, long issueNs, long vpStart, String offerId, boolean isWarmup) {
        String tag = iterTag(index, isWarmup);
        Map<String, String> reqData = new HashMap<>();
        reqData.put("pin", AUTO_PIN);
        reqData.put("offerId", offerId);
        reqData.put("payloadType", "Direct");

        VpSubmission vp = new VpSubmission(this, reqData);
        vp.setCallback(new BaseOperation.Callback() {
            @Override
            public void onSuccess(ProtocolData preData) {
                vp.setCallback(new BaseOperation.Callback() {
                    @Override
                    public void onSuccess(ProtocolData execData) {
                        long vpNs = System.nanoTime() - vpStart;
                        appendStatus("  P310 완료: " + msStr(vpNs));
                        Log.i(TAG, tag + " VP " + msStr(vpNs));

                        aConsecErrors = 0;
                        long totalNs = regNs + issueNs + vpNs;
                        if (!isWarmup) {
                            results.add(new long[]{regNs, issueNs, vpNs});
                        }

                        String summary = String.format("  전체: REG=%s ISSUE=%s VP=%s TOTAL=%s",
                                msStr(regNs), msStr(issueNs), msStr(vpNs), msStr(totalNs));
                        appendStatus(summary);
                        Log.i(TAG, tag + " ITER_DONE"
                                + " reg=" + ns2ms(regNs) + "ms"
                                + " issue=" + ns2ms(issueNs) + "ms"
                                + " vp=" + ns2ms(vpNs) + "ms"
                                + " total=" + ns2ms(totalNs) + "ms");

                        advanceIteration(index, isWarmup);
                    }

                    @Override
                    public void onFailure(int code, String msg) {
                        appendStatus("  [P310 execute 오류] " + code + ": " + msg);
                        Log.e(TAG, tag + " VP execute FAIL " + code + " " + msg);
                        aConsecErrors++;
                        if (aConsecErrors >= A_MAX_CONSECUTIVE_ERRORS) { abortScenarioA(); return; }
                        advanceIteration(index, isWarmup);
                    }
                }).execute();
            }

            @Override
            public void onFailure(int code, String msg) {
                appendStatus("  [P310 preExecute 오류] " + code + ": " + msg);
                Log.e(TAG, tag + " VP preExecute FAIL " + code + " " + msg);
                aConsecErrors++;
                if (aConsecErrors >= A_MAX_CONSECUTIVE_ERRORS) { abortScenarioA(); return; }
                advanceIteration(index, isWarmup);
            }
        }).preExecute();
    }

    // ────────────────────────────────────────────────
    // 결과 리포트
    // ────────────────────────────────────────────────

    private void printReport() {
        // 테스트 완료 후 SDK 알고리즘 복원
        org.omnione.did.sdk.wallet.walletservice.config.Config.KEY_ALGORITHM = SDK_ALGO_DEFAULT;
        Log.i(TAG, "Test complete: KEY_ALGORITHM restored to " + SDK_ALGO_DEFAULT);

        setRunning(false);

        if (results.isEmpty()) {
            appendStatus("\n[리포트] 성공한 측정 반복이 없습니다.");
            Log.i(TAG, "=== REPORT: no measurement results ===");
            ContextCompat.getMainExecutor(this).execute(() -> {
                if (startButton != null) {
                    startButton.setEnabled(true);
                    startButton.setText("다시 테스트");
                    startButton.setOnClickListener(v -> showScenarioAConfig());
                }
            });
            return;
        }

        int n = results.size();
        long[] regArr = new long[n];
        long[] issueArr = new long[n];
        long[] vpArr = new long[n];
        long[] totalArr = new long[n];

        for (int i = 0; i < n; i++) {
            long[] r = results.get(i);
            regArr[i] = r[0];
            issueArr[i] = r[1];
            vpArr[i] = r[2];
            totalArr[i] = r[0] + r[1] + r[2];
        }

        Arrays.sort(regArr);
        Arrays.sort(issueArr);
        Arrays.sort(vpArr);
        Arrays.sort(totalArr);

        long avgReg = mean(regArr);
        long avgIssue = mean(issueArr);
        long avgVp = mean(vpArr);
        long avgTotal = mean(totalArr);

        long p50Reg = calcP50(regArr),   p50Issue = calcP50(issueArr),   p50Vp = calcP50(vpArr),   p50Total = calcP50(totalArr);
        long p95Reg = calcP95(regArr),   p95Issue = calcP95(issueArr),   p95Vp = calcP95(vpArr),   p95Total = calcP95(totalArr);
        long minReg = regArr[0],         minIssue = issueArr[0],         minVp = vpArr[0],         minTotal = totalArr[0];
        long maxReg = regArr[n - 1],     maxIssue = issueArr[n - 1],     maxVp = vpArr[n - 1],     maxTotal = totalArr[n - 1];

        String report = String.format(
                "\n========== PQC Auto Test Report ==========\n" +
                "알고리즘: %s\n" +
                "vcPlanId: %s\n" +
                "Warmup: %d회 (제외) | Measurement: %d회\n" +
                "policyId: %s\n\n" +
                "%-10s %10s %10s %10s %10s\n" +
                "%-10s %10s %10s %10s %10s\n" +
                "%-10s %10s %10s %10s %10s\n" +
                "%-10s %10s %10s %10s %10s\n" +
                "%-10s %10s %10s %10s %10s\n" +
                "%-10s %10s %10s %10s %10s\n" +
                "==========================================",
                selectedAlgorithm,
                selectedVcPlanIdA,
                warmupCount, n,
                selectedVpPolicyIdA,
                "",      "P132",             "P210",               "P310",            "TOTAL",
                "mean",  msStr(avgReg),      msStr(avgIssue),      msStr(avgVp),      msStr(avgTotal),
                "p50",   msStr(p50Reg),      msStr(p50Issue),      msStr(p50Vp),      msStr(p50Total),
                "p95",   msStr(p95Reg),      msStr(p95Issue),      msStr(p95Vp),      msStr(p95Total),
                "min",   msStr(minReg),      msStr(minIssue),      msStr(minVp),      msStr(minTotal),
                "max",   msStr(maxReg),      msStr(maxIssue),      msStr(maxVp),      msStr(maxTotal)
        );

        appendStatus(report);
        Log.i(TAG, report.replace("\n", " | "));
        Log.i(TAG, String.format(
                "SUMMARY algo=%s n=%d warmup=%d "
                        + "meanTotal=%dms p50Total=%dms p95Total=%dms minTotal=%dms maxTotal=%dms",
                selectedAlgorithm, n, warmupCount,
                ns2ms(avgTotal), ns2ms(p50Total), ns2ms(p95Total), ns2ms(minTotal), ns2ms(maxTotal)));

        java.util.LinkedHashMap<String, long[]> metrics = new java.util.LinkedHashMap<>();
        metrics.put("reg",   regArr);
        metrics.put("issue", issueArr);
        metrics.put("vp",    vpArr);
        metrics.put("total", totalArr);
        sendReportToObserver("A", selectedAlgorithm, selectedVcPlanIdA, selectedVpPolicyIdA, n, metrics);

        ContextCompat.getMainExecutor(this).execute(() -> {
            if (startButton != null) {
                startButton.setEnabled(true);
                startButton.setText("다시 테스트");
                startButton.setOnClickListener(v -> showScenarioAConfig());
            }
        });
    }

    // ────────────────────────────────────────────────
    // Observer 리포트 전송
    // ────────────────────────────────────────────────

    private void sendReportToObserver(String scenario, String algorithm,
                                       String vcPlanId, String vpPolicyId,
                                       int n, java.util.LinkedHashMap<String, long[]> sortedMetrics) {
        try {
            org.json.JSONObject metricsJson = new org.json.JSONObject();
            for (java.util.Map.Entry<String, long[]> entry : sortedMetrics.entrySet()) {
                long[] arr = entry.getValue();
                org.json.JSONObject m = new org.json.JSONObject();
                m.put("mean", ns2msD(mean(arr)));
                m.put("p50",  ns2msD(calcP50(arr)));
                m.put("p95",  ns2msD(calcP95(arr)));
                m.put("min",  ns2msD(arr[0]));
                m.put("max",  ns2msD(arr[arr.length - 1]));
                metricsJson.put(entry.getKey(), m);
            }
            org.json.JSONObject body = new org.json.JSONObject();
            body.put("scenario", scenario);
            body.put("algorithm", algorithm);
            if (vcPlanId != null) body.put("vcPlanId", vcPlanId);
            if (vpPolicyId != null) body.put("vpPolicyId", vpPolicyId);
            body.put("warmupCount", warmupCount);
            body.put("measurementCount", n);
            body.put("metrics", metricsJson);
            String bodyStr = body.toString();

            new Thread(() -> {
                try {
                    java.net.URL url = new java.net.URL(Config.Observer.REPORT_URL);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    try (java.io.OutputStream os = conn.getOutputStream()) {
                        os.write(bodyStr.getBytes("utf-8"));
                    }
                    conn.getResponseCode();
                    conn.disconnect();
                    CaLog.d("Report sent to observer: scenario=" + scenario);
                } catch (Exception e) {
                    CaLog.d("Observer report send failed (silent): " + e.getMessage());
                }
            }).start();
        } catch (Exception e) {
            CaLog.e("sendReportToObserver build error: " + e.getMessage());
        }
    }

    // ────────────────────────────────────────────────
    // 통계 헬퍼
    // ────────────────────────────────────────────────

    private static long mean(long[] values) {
        long sum = 0;
        for (long v : values) sum += v;
        return sum / values.length;
    }

    long calcP50(long[] sorted) {
        return percentile(sorted, 0.50d);
    }

    long calcP95(long[] sorted) {
        return percentile(sorted, 0.95d);
    }

    // Linear interpolation between sorted samples (type 7).
    private static long percentile(long[] sorted, double pct) {
        int n = sorted.length;
        if (n == 0) return 0L;
        if (n == 1) return sorted[0];

        double rank = pct * (n - 1);
        int lo = (int) Math.floor(rank);
        int hi = (int) Math.ceil(rank);
        if (lo == hi) return sorted[lo];

        double frac = rank - lo;
        double interp = sorted[lo] + (sorted[hi] - sorted[lo]) * frac;
        return Math.round(interp);
    }

    private static String buildVcSchemaId(String vcPlanId) {
        return "vc.schema." + vcPlanId;
    }

    // ────────────────────────────────────────────────
    // UI 상태 헬퍼
    // ────────────────────────────────────────────────

    private void setRunning(boolean running) {
        isRunning = running;
        ContextCompat.getMainExecutor(this).execute(() -> {
            ActionBar bar = getSupportActionBar();
            if (bar != null) {
                bar.setDisplayHomeAsUpEnabled(!running);
            }
        });
    }

    private void setStatus(String text) {
        ContextCompat.getMainExecutor(this).execute(() -> {
            if (statusView == null) return;
            statusView.setText(text);
            if (scrollView != null) scrollView.fullScroll(ScrollView.FOCUS_DOWN);
        });
    }

    private void appendStatus(String text) {
        ContextCompat.getMainExecutor(this).execute(() -> {
            if (statusView == null) return;
            statusView.append("\n" + text);
            if (scrollView != null) scrollView.fullScroll(ScrollView.FOCUS_DOWN);
        });
    }

    // ────────────────────────────────────────────────
    // 단위 변환 헬퍼
    // ────────────────────────────────────────────────

    private static long ns2ms(long ns) {
        return ns / 1_000_000;
    }

    private static String msStr(long ns) {
        return ns2ms(ns) + "ms";
    }
}
