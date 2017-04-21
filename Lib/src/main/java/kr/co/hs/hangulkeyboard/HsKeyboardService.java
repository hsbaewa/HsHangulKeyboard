package kr.co.hs.hangulkeyboard;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.method.MetaKeyKeyListener;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * 생성된 시간 2017-04-21, Bae 에 의해 생성됨
 * 프로젝트 이름 : HsHangulKeyboard
 * 패키지명 : kr.co.hs.hangulkeyboard
 */

public class HsKeyboardService extends InputMethodService implements KeyboardView.OnKeyboardActionListener{
    /**
     * This boolean indicates the optional example code for performing
     * processing of hard keys in addition to regular text generation
     * from on-screen interaction.  It would be used for input methods that
     * perform language translations (such as converting text entered on
     * a QWERTY keyboard to Chinese), but may not be used for input methods
     * that are primarily intended to be used for on-screen text entry.
     */
    static final boolean PROCESS_HARD_KEYS = true;
    protected static final int LANG_ENG = 0;
    protected static final int LANG_KOR = 100;

    protected View mInputView;
    protected HsKeyboardView mKeyboardView;
    protected HsKeyboard mEngKeyboard;
    protected HsKeyboard mEngShiftedKeyboard;
    protected HsKeyboard mSymbolsKeyboard;
    protected HsKeyboard mSymbolsShiftedKeyboard;

    private int mLastLanguage = 0;
    protected HsKeyboard mCurrentKeyboard;

    private final StringBuilder mComposing = new StringBuilder();
    private boolean mPredictionOn;
    private boolean mCapsLock;
    private boolean mCompletionOn;
    private long mMetaState;
    private CompletionInfo[] mCompletions;
    private String mWordSeparators;
    private long mLastShiftTime;

    //마지막으로 알고 있는 키보드 너비
    private int mLastDisplayWidth;

    @Override
    public void onCreate() {
        super.onCreate();
        mWordSeparators = getResources().getString(R.string.word_separators);
    }

    @Override
    public void onInitializeInterface() {
        super.onInitializeInterface();
        if(mEngKeyboard != null){
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        }

        mEngKeyboard = new HsKeyboard(this, R.xml.eng);
        mEngShiftedKeyboard = new HsKeyboard(this, R.xml.eng_shift);
        mSymbolsKeyboard = new HsKeyboard(this, R.xml.symbols);
        mSymbolsShiftedKeyboard = new HsKeyboard(this, R.xml.symbols_shift);
    }

    @Override
    public View onCreateInputView() {
        mInputView = getLayoutInflater().inflate(R.layout.kr_co_hs_hskeyboardlayout, null);
        mKeyboardView = (HsKeyboardView) mInputView.findViewById(R.id.KeyboardView);
        mKeyboardView.setOnKeyboardActionListener(this);
        mKeyboardView.setKeyboard(getCurrentKeyboard());
        return mInputView;
    }

    @Override
    public View onCreateCandidatesView() {
        //        mCandidateView = new CandidateView(this);
//        mCandidateView.setService(this);
//        return mCandidateView;
        return null;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);

        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        getComposing().setLength(0);
        updateCandidates();

        //옵션?
        mPredictionOn = false;
        mCompletionOn = false;

        switch (attribute.inputType&EditorInfo.TYPE_MASK_CLASS) {
            case EditorInfo.TYPE_CLASS_NUMBER:
            case EditorInfo.TYPE_CLASS_DATETIME:{
                //숫자 또는 날짜 입력
                setCurrentKeyboard(mSymbolsKeyboard);
                break;
            }
            case EditorInfo.TYPE_CLASS_PHONE:{
                //전화번호
                setCurrentKeyboard(mSymbolsKeyboard);
                break;
            }
            case EditorInfo.TYPE_CLASS_TEXT:{
                //일반 텍스트
                mPredictionOn = true;

                setCurrentKeyboard(getQwertyKeyboard());

                int variation = attribute.inputType &  EditorInfo.TYPE_MASK_VARIATION;
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mPredictionOn = false;
                }

                if (variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_URI
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    mPredictionOn = false;
                }

                if ((attribute.inputType&EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
                    mPredictionOn = false;
                    mCompletionOn = isFullscreenMode();
                }

                // We also want to look at the current state of the editor
                // to decide whether our alphabetic keyboard should start out
                // shifted.
                updateShiftKeyState(attribute);
                break;
            }
            default:{
               //그 외
                setCurrentKeyboard(getQwertyKeyboard());
                break;
            }
        }
        getCurrentKeyboard().setImeOptions(getResources(), attribute.imeOptions);
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();
        // Clear current composing text and candidates.
        getComposing().setLength(0);
        updateCandidates();

        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false);

//        mCurKeyboard = mQwertyKeyboard;
        if (mKeyboardView != null) {
            mKeyboardView.closing();
        }
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        // Apply the selected keyboard to the input view.
        mKeyboardView.setKeyboard(getCurrentKeyboard());
        mKeyboardView.closing();
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        if (getComposing().length() > 0 && (newSelStart != candidatesEnd
                || newSelEnd != candidatesEnd)) {
            getComposing().setLength(0);
            updateCandidates();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
        }
    }



    @Override
    public void onPress(int primaryCode) {

    }

    @Override
    public void onRelease(int primaryCode) {

    }

    protected boolean handleWordSeparator(HsKeyboard keyboard, int primaryCode, int[] keycodes){
        // Handle separator
        if (getComposing().length() > 0) {
            commitTyped(getCurrentInputConnection());
        }
        sendKey(primaryCode);
        updateShiftKeyState(getCurrentInputEditorInfo());
        return true;
    }

    protected boolean handleModeChange(HsKeyboard current){
        if (current == mSymbolsKeyboard || current == mSymbolsShiftedKeyboard) {
            current = getQwertyKeyboard();
        } else {
            current = getSymbolsKeyboard();
        }
        mKeyboardView.setKeyboard(current);
        if (current == mSymbolsKeyboard) {
            current.setShifted(false);
        }
        return true;
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        Log.i("Hangul", "onKey PrimaryCode[" + Integer.toString(primaryCode)+"]");

        HsKeyboard keyboard = (HsKeyboard) mKeyboardView.getKeyboard();
        if(isWordSeparator(primaryCode)){
            handleWordSeparator(keyboard, primaryCode, keyCodes);
        }else if(primaryCode == Keyboard.KEYCODE_DELETE){
            handleBackspace(keyboard);
        }else if(primaryCode == Keyboard.KEYCODE_SHIFT){
            handleShift(keyboard);
        }else if(primaryCode == Keyboard.KEYCODE_CANCEL){
            handleClose(keyboard);
        }else if(primaryCode == HsKeyboard.KEYCODE_OPTIONS) {

        }else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE) {
            handleModeChange(keyboard);
        } else if(primaryCode == HsKeyboard.KEYCODE_CHANGE_LANGUAGE){
            handleChangeLanguage(keyboard);
        } else {
            handleCharacter(primaryCode, keyCodes);
        }
    }

    @Override
    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        if (getComposing().length() > 0) {
            commitTyped(ic);
        }
        ic.commitText(text, 0);
        ic.endBatchEdit();
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    @Override
    public void swipeLeft() {
        HsKeyboard keyboard = (HsKeyboard) mKeyboardView.getKeyboard();
        handleBackspace(keyboard);
    }

    @Override
    public void swipeRight() {
        if (mCompletionOn) {
            pickDefaultCandidate();
        }
    }

    @Override
    public void swipeDown() {
        HsKeyboard keyboard = (HsKeyboard) mKeyboardView.getKeyboard();
        handleClose(keyboard);
    }

    @Override
    public void swipeUp() {

    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode){
            case KeyEvent.KEYCODE_BACK:{
                // The InputMethodService already takes care of the back
                // key for us, to dismiss the input method if it is shown.
                // However, our keyboard could be showing a pop-up window
                // that back should dismiss, so we first allow it to do that.
                if (event.getRepeatCount() == 0 && mKeyboardView != null) {
                    if (mKeyboardView.handleBack()) {
                        return true;
                    }
                }
                break;
            }
            case KeyEvent.KEYCODE_DEL:{
                // Special handling of the delete key: if we currently are
                // composing text for the user, we want to modify that instead
                // of let the application to the delete itself.
                if (getComposing().length() > 0) {
                    onKey(Keyboard.KEYCODE_DELETE, null);
                    return true;
                }
                break;
            }
            case KeyEvent.KEYCODE_ENTER:{
                // Let the underlying text editor always handle these.
                return false;
            }
            default:{
                // For all other keys, if we want to do transformations on
                // text being entered with a hard keyboard, we need to process
                // it and do the appropriate action.
                if (PROCESS_HARD_KEYS) {
                    if (keyCode == KeyEvent.KEYCODE_SPACE
                            && (event.getMetaState()&KeyEvent.META_ALT_ON) != 0) {
                        // A silly example: in our input method, Alt+Space
                        // is a shortcut for 'android' in lower case.
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) {
                            // First, tell the editor that it is no longer in the
                            // shift state, since we are consuming this.
                            ic.clearMetaKeyStates(KeyEvent.META_ALT_ON);
                            keyDownUp(KeyEvent.KEYCODE_A);
                            keyDownUp(KeyEvent.KEYCODE_N);
                            keyDownUp(KeyEvent.KEYCODE_D);
                            keyDownUp(KeyEvent.KEYCODE_R);
                            keyDownUp(KeyEvent.KEYCODE_O);
                            keyDownUp(KeyEvent.KEYCODE_I);
                            keyDownUp(KeyEvent.KEYCODE_D);
                            // And we consume this event.
                            return true;
                        }
                    }
                    if (mPredictionOn && translateKeyDown(keyCode, event)) {
                        return true;
                    }
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.
        if (PROCESS_HARD_KEYS) {
            if (mPredictionOn) {
                mMetaState = MetaKeyKeyListener.handleKeyUp(mMetaState,
                        keyCode, event);
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onDisplayCompletions(CompletionInfo[] completions) {
        if (mCompletionOn) {
            mCompletions = completions;
            if (completions == null) {
                setSuggestions(null, false, false);
                return;
            }

            List<String> stringList = new ArrayList<String>();
            for (int i=0; i<(completions != null ? completions.length : 0); i++) {
                CompletionInfo ci = completions[i];
                if (ci != null) stringList.add(ci.getText().toString());
            }
            setSuggestions(stringList, true, true);
        }
    }

///////////////
    private HsKeyboard getSymbolsKeyboard(){
        return mSymbolsKeyboard;
    }
    protected int getLastLanguage(){
        return mLastLanguage;
    }
    protected void setLastLanguage(int language){
        this.mLastLanguage = language;
    }
    protected HsKeyboard getQwertyKeyboard(){
        switch (getLastLanguage()){
            default:{
                return mEngKeyboard;
            }
        }
    }
    protected HsKeyboard getCurrentKeyboard(){
        if(mCurrentKeyboard == null)
            mCurrentKeyboard = mEngKeyboard;
        return mCurrentKeyboard;
    }
    protected boolean setCurrentKeyboard(HsKeyboard keyboard){
        if(isEngKeyboard(keyboard)){
            this.mCurrentKeyboard = keyboard;
            setLastLanguage(LANG_ENG);
            return true;
        }
        return false;
    }
    protected boolean isEngKeyboard(HsKeyboard keyboard){
        if(keyboard == mEngKeyboard || keyboard == mEngShiftedKeyboard)
            return true;
        else
            return false;
    }

    private boolean isSymbolKeyboard(HsKeyboard keyboard){
        if(keyboard == mSymbolsKeyboard || keyboard == mSymbolsShiftedKeyboard)
            return true;
        else
            return false;
    }

    protected boolean handleChangeLanguage(HsKeyboard currentKeyboard) {
        if(currentKeyboard == mSymbolsKeyboard){
            currentKeyboard.setShifted(false);
        }
        return false;
    }
    protected StringBuilder getComposing(){
        return mComposing;
    }
    ////////////////////////

    protected boolean handleCharacter(int primaryCode, int[] keyCodes) {
        HsKeyboard current = (HsKeyboard) mKeyboardView.getKeyboard();
        if(isEngKeyboard(current)){
            if (isInputViewShown()) {
                if (mKeyboardView.isShifted()) {
                    primaryCode = Character.toUpperCase(primaryCode);
                }
            }
            if (isAlphabet(primaryCode) && mPredictionOn) {
                getComposing().append((char) primaryCode);
                getCurrentInputConnection().setComposingText(getComposing(), 1);
                updateShiftKeyState(getCurrentInputEditorInfo());
                updateCandidates();
            } else {
                sendKeyChar((char)primaryCode);
            }
            return true;
        }
        return false;
    }

    /**
     * Helper to determine if a given character code is alphabetic.
     */
    private boolean isAlphabet(int code) {
        if (Character.isLetter(code)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    protected void updateShiftKeyState(EditorInfo attr) {
        if (attr != null && mInputView != null && isEngKeyboard((HsKeyboard) mKeyboardView.getKeyboard())) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != EditorInfo.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }
            mKeyboardView.setShifted(mCapsLock || caps != 0);
        }
    }

    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    protected void updateCandidates() {
        if (!mCompletionOn) {
            if (getComposing().length() > 0) {
                ArrayList<String> list = new ArrayList<String>();
                list.add(getComposing().toString());
                setSuggestions(list, true, true);
            } else {
                setSuggestions(null, false, false);
            }
        }
    }

    public void setSuggestions(List<String> suggestions, boolean completions,
                               boolean typedWordValid) {
        /*
        if (suggestions != null && suggestions.size() > 0) {
            setCandidatesViewShown(true);
        } else if (isExtractViewShown()) {
            setCandidatesViewShown(true);
        }
        if (mCandidateView != null) {
            mCandidateView.setSuggestions(suggestions, completions, typedWordValid);
        }
        */
    }


    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    protected void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }


    /**
     * Helper to send a character to the editor as raw key events.
     */
    protected void sendKey(int keyCode) {
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }


    /**
     * This translates incoming hard key events in to edit operations on an
     * InputConnection.  It is only needed when using the
     * PROCESS_HARD_KEYS option.
     */
    private boolean translateKeyDown(int keyCode, KeyEvent event) {
        mMetaState = MetaKeyKeyListener.handleKeyDown(mMetaState,
                keyCode, event);
        int c = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(mMetaState));
        mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
        InputConnection ic = getCurrentInputConnection();
        if (c == 0 || ic == null) {
            return false;
        }

        boolean dead = false;

        if ((c & KeyCharacterMap.COMBINING_ACCENT) != 0) {
            dead = true;
            c = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
        }

        if (getComposing().length() > 0) {
            char accent = getComposing().charAt(getComposing().length() -1 );
            int composed = KeyEvent.getDeadChar(accent, c);

            if (composed != 0) {
                c = composed;
                getComposing().setLength(getComposing().length()-1);
            }
        }

        onKey(c, null);

        return true;
    }

    public void pickDefaultCandidate() {
        pickSuggestionManually(0);
    }
    public void pickSuggestionManually(int index) {
        if (mCompletionOn && mCompletions != null && index >= 0
                && index < mCompletions.length) {
            CompletionInfo ci = mCompletions[index];
            getCurrentInputConnection().commitCompletion(ci);
            /*
            if (mCandidateView != null) {
                mCandidateView.clear();
            }
            */
            updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (getComposing().length() > 0) {
            // If we were generating candidate suggestions for the current
            // text, we would commit one of them here.  But for this sample,
            // we will just commit the current text.
            commitTyped(getCurrentInputConnection());
        }
    }

    /**
     * Helper function to commit any text being composed in to the editor.
     */
    protected void commitTyped(InputConnection inputConnection) {
        if (getComposing().length() > 0) {
            inputConnection.commitText(getComposing(), getComposing().length());
            getComposing().setLength(0);
            updateCandidates();
        }
    }

    protected boolean handleBackspace(HsKeyboard keyboard) {
        final int length = getComposing().length();
        if (length > 1) {
            getComposing().delete(length - 1, length);
            getCurrentInputConnection().setComposingText(getComposing(), 1);
            updateCandidates();
        } else if (length > 0) {
            getComposing().setLength(0);
            getCurrentInputConnection().commitText("", 0);
            updateCandidates();
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL);
            //뒤에서부터 삭제
//            keyDownUp(KeyEvent.KEYCODE_FORWARD_DEL);
        }
        updateShiftKeyState(getCurrentInputEditorInfo());
        return true;
    }

    protected boolean handleShift(HsKeyboard keyboard) {
        if (mInputView == null) {
            return false;
        }

        HsKeyboard currentKeyboard = (HsKeyboard) mKeyboardView.getKeyboard();
        if(isEngKeyboard(currentKeyboard)){
            // Alphabet keyboard
            checkToggleCapsLock();
            mKeyboardView.setShifted(mCapsLock || !mKeyboardView.isShifted());
            return true;
        } else if (currentKeyboard == mSymbolsKeyboard) {
            mSymbolsKeyboard.setShifted(true);
            mKeyboardView.setKeyboard(mSymbolsShiftedKeyboard);
            mSymbolsShiftedKeyboard.setShifted(true);
            return true;
        } else if (currentKeyboard == mSymbolsShiftedKeyboard) {
            mSymbolsShiftedKeyboard.setShifted(false);
            mKeyboardView.setKeyboard(mSymbolsKeyboard);
            mSymbolsKeyboard.setShifted(false);
            return true;
        }
        return false;
    }

    private void checkToggleCapsLock() {
        long now = System.currentTimeMillis();
        if (mLastShiftTime + 800 > now) {
            mCapsLock = !mCapsLock;
            mLastShiftTime = 0;
        } else {
            mLastShiftTime = now;
        }
    }

    protected void handleClose(HsKeyboard keyboard) {
        commitTyped(getCurrentInputConnection());
        requestHideSelf(0);
        mKeyboardView.closing();
    }

    private String getWordSeparators() {
        return mWordSeparators;
    }

    public boolean isWordSeparator(int code) {
        String separators = getWordSeparators();
        return separators.contains(String.valueOf((char)code));
    }
}
