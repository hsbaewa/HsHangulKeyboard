package kr.co.hs.hangulkeyboard.hangul;

import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import kr.co.hs.hangulkeyboard.HsKeyboard;
import kr.co.hs.hangulkeyboard.HsKeyboardService;
import kr.co.hs.hangulkeyboard.R;

/**
 * 생성된 시간 2017-04-21, Bae 에 의해 생성됨
 * 프로젝트 이름 : HsHangulKeyboard
 * 패키지명 : kr.co.hs.hangulkeyboard
 */

public class HsHangulKeyboardService extends HsKeyboardService {
    private HsKeyboard mHangulKeyboard;
    private HsKeyboard mHangulShiftedKeyboard;


    private int mHangulShiftState = 0;
    private int mHangulState = 0;
    private int previousHangulCurPos = -1;
    private int previousCurPos = -2;

    private static char HCURSOR_NONE = 0;
    private static char HCURSOR_NEW = 1;
    private static char HCURSOR_ADD = 2;
    private static char HCURSOR_UPDATE = 3;
    private static char HCURSOR_APPEND = 4;
    private static char HCURSOR_UPDATE_LAST = 5;
    private static char HCURSOR_DELETE_LAST = 6;
    private static char HCURSOR_DELETE = 7;

    final static int H_STATE_0 = 0;
    final static int H_STATE_1 = 1;
    final static int H_STATE_2 = 2;
    final static int H_STATE_3 = 3;
    final static int H_STATE_4 = 4;
    final static int H_STATE_5 = 5;
    final static int H_STATE_6 = 6;

    final static char KO_S_0000 = 0;
    final static char KO_S_0100 = 1;
    final static char KO_S_1000 = 2;
    final static char KO_S_1100 = 3;
    final static char KO_S_1110 = 4;
    final static char KO_S_1111 = 5;

    private int prev_key = -1;
    private char ko_state_idx = KO_S_0000;
    private char ko_state_first_idx;
    private char ko_state_middle_idx;
    private char ko_state_last_idx;
    private char ko_state_next_idx;

    private static int mHCursorState = HCURSOR_NONE;
    private static int mHangulKeyStack[] = {0,0,0,0,0,0};
    private static int mHangulJamoStack[] = {0,0,0};

    final private char jungsung_stack[] = {
            // 1  2 3  4  5  6  7 8  9  10 11 12 13 14 15 16 17 18 19 20 21 22 23
            // . .. ��,��,��,��,��,��,��,��,��,��,��, ��,��,��,��,��,��,��,��,��, ��
            0,0, 0, 0, 0, 0, 0, 0,0, 0, 0, 11,11,11, 0, 0,16,16,16,0, 0, 21, 0
    };
    final private char chosung_code[] = {
            0, 1, 3, 6, 7, 8, 16, 17, 18, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29
    };
    final private char jongsung_28idx[] = {
            0, 0, 1, 1, 4, 4, 4, 8, 8, 8, 8, 17, 17, 0, 8, 8, 8
    };


    final static char[] h_chosung_idx =
            {0,1, 9,2,12,18,3, 4,5, 0, 6,7, 9,16,17,18,6, 7, 8, 9,9,10,11,12,13,14,15,16,17,18};
    /*
        {0, 1, 9, 2,12,18, 3,4, 5, 0, 6, 7, 9,16,17, 18,6, 7, 8, 9, 9,10,11, 12, 13,14,15,16,17,18};
    //   ��,��,��,��,��,��,��,��,��,��,��,��,��,��,��, ��,��,��,��,��,��,��,��, ��, ��,��,��, ��,��,��
    //   ��,��,   ��,      ��,��,��,                     ��,��,��,   ��,��,��, ��, ��,��,��, ��,��,��
    */
    final static char[] h_jongsung_idx =
            {0, 1, 2, 3,4,5, 6, 7, 0,8, 9,10,11,12,13,14,15,16,17,0 ,18,19,20,21,22,0 ,23,24,25,26,27};
/*
    {0, 1, 2, 3, 4, 5, 6, 7, 0,8, 9,10,11, 12,13, 14,15,16, 17,0,18, 19,20,21,22, 0 ,23,24,25,26,27};
//   x, ��,��,��,��,��,��,��,��,��,��,��,��, ��,��, ��,��,��, ��,��,��, ��,��, o,��, ��,��, ��,��,��,��,
	//  x  ��  ��  ��  ��  ��  ��  ��       ��  ��  ��  ��    ��  ��   ��   ��  ��    ��        ��   ��  ��   ��   ��         ��   ��   ��  ��  ��

	//	��,��,��,	��,��,��,	��,��,��,��,��,��,��,��,��,��,��,��,��,��,��
*/

    final static int[] e2h_map =
            {16,47,25,22,6, 8,29,38,32,34,30,50,48,43,31,35,17,0, 3,20,36,28,23,27,42,26,
                    16,47,25,22,7, 8,29,38,32,34,30,50,48,43,33,37,18,1, 3,21,36,28,24,27,42,26};

    final private char ko_jong_m_split[] = {
            0,1, 0,2, 1,10, 0,3, 4,13,
            4,19, 0,4, 0,6, 8,1, 8,7,
            8,8, 8,10, 8,17, 8,18, 8,19,
            0,7,0,8,17,10,0,10,0,11,
            0,12,0,13,0,15,0,16,0,17,
            0,18,0,19
    };


    @Override
    public void onInitializeInterface() {
        super.onInitializeInterface();
        mHangulKeyboard = new HsKeyboard(this, R.xml.hangul);
        mHangulShiftedKeyboard = new HsKeyboard(this, R.xml.hangul_shift);
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        clearHangul();
        clearSejong();
        previousCurPos = -1;

        super.onStartInput(attribute, restarting);
    }

    protected boolean isHangulKeyboard(HsKeyboard keyboard){
        if(keyboard == mHangulKeyboard || keyboard == mHangulShiftedKeyboard)
            return true;
        else
            return false;
    }

    @Override
    protected HsKeyboard getQwertyKeyboard() {
        switch (getLastLanguage()){
            case LANG_KOR:{
                return mHangulKeyboard;
            }
            default:{
                return super.getQwertyKeyboard();
            }
        }
    }

    @Override
    protected boolean setCurrentKeyboard(HsKeyboard keyboard) {
        if(isHangulKeyboard(keyboard)){
            this.mCurrentKeyboard = keyboard;
            setLastLanguage(LANG_KOR);
            return true;
        }
        return super.setCurrentKeyboard(keyboard);
    }

    @Override
    protected boolean handleChangeLanguage(HsKeyboard currentKeyboard) {
        if(!super.handleChangeLanguage(currentKeyboard)){
            if(isEngKeyboard(currentKeyboard)){
                setCurrentKeyboard(mHangulKeyboard);
            }else if(isHangulKeyboard(currentKeyboard)){
                setCurrentKeyboard(mEngKeyboard);
            }
            mKeyboardView.setKeyboard(getCurrentKeyboard());
            return true;
        }
        return false;
    }

    @Override
    protected boolean handleWordSeparator(HsKeyboard keyboard, int primaryCode, int[] keycodes) {
        if(isHangulKeyboard(keyboard)){
            // Handle separator
            if (getComposing().length() > 0) {
                commitTyped(getCurrentInputConnection());
            }
            clearHangul();
            sendKey(primaryCode);
            updateShiftKeyState(getCurrentInputEditorInfo());
            return true;
        }else{
            return super.handleWordSeparator(keyboard, primaryCode, keycodes);
        }
    }

    @Override
    protected boolean handleBackspace(HsKeyboard keyboard) {
        if(isHangulKeyboard(keyboard)){
            hangulSendKey(-2,HCURSOR_NONE);
            return true;
        }else{
            return super.handleBackspace(keyboard);
        }
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        HsKeyboard current = (HsKeyboard) mKeyboardView.getKeyboard();
        if(isHangulKeyboard(current)){
            Log.i("Hangul", "onUpdateSelection :"
                    + Integer.toString(oldSelStart) + ":"
                    + Integer.toString(oldSelEnd) + ":"
                    + Integer.toString(newSelStart) + ":"
                    + Integer.toString(newSelEnd) + ":"
                    + Integer.toString(candidatesStart) + ":"
                    + Integer.toString(candidatesEnd)
            );

            if (getComposing().length() > 0 && (newSelStart != candidatesEnd
                    || newSelEnd != candidatesEnd)) {
                getComposing().setLength(0);
//	            updateCandidates();
                clearHangul();
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    ic.finishComposingText();
                }
            }
        }else{
            super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
        }
    }

    @Override
    protected boolean handleCharacter(int primaryCode, int[] keyCodes) {
        HsKeyboard keyboard = (HsKeyboard) mKeyboardView.getKeyboard();
        if(isHangulKeyboard(keyboard)){
            handleHangul(primaryCode, keyCodes);
            return true;
        }else{
            boolean result = super.handleCharacter(primaryCode, keyCodes);
            return result;
        }
    }



    @Override
    protected boolean handleShift(HsKeyboard keyboard) {
        if(mKeyboardView == null)
            return false;
        if(keyboard == mHangulKeyboard){
            mHangulKeyboard.setShifted(true);
            mKeyboardView.setKeyboard(mHangulShiftedKeyboard);
            mHangulShiftedKeyboard.setShifted(true);
            mHangulShiftState = 1;
            return true;
        } else if (keyboard == mHangulShiftedKeyboard) {
            mHangulShiftedKeyboard.setShifted(false);
            mKeyboardView.setKeyboard(mHangulKeyboard);
            mHangulKeyboard.setShifted(false);
            mHangulShiftState = 0;
            return true;
        }
        return super.handleShift(keyboard);
    }



    private void clearHangul() {
        mHCursorState = HCURSOR_NONE;
        mHangulState = 0;
        previousHangulCurPos = -1;
        mHangulKeyStack[0] = 0;
        mHangulKeyStack[1] = 0;
        mHangulKeyStack[2] = 0;
        mHangulKeyStack[3] = 0;
        mHangulKeyStack[4] = 0;
        mHangulKeyStack[5] = 0;
        mHangulJamoStack[0] = 0;
        mHangulJamoStack[1] = 0;
        mHangulJamoStack[2] = 0;
        return;
    }

    private int isHangulKey(int stack_pos, int new_key) {
    	/*
        MAP(0,20,1); // ��,��
    	MAP(3,23,4); // ��,��
    	MAP(3,29,5); // ��,��
    	MAP(8,0,9); // ��,��
    	MAP(8,16,10); // ��,��
    	MAP(8,17,11); // ��,��
    	MAP(8,20,12); // ��,��
    	MAP(8,27,13); // ��,��
    	MAP(8,28,14); // ��,��
    	MAP(8,29,15); // ��,��
    	MAP(17,20,19); // ��,��
    */
        if (stack_pos != 2) {
            switch (mHangulKeyStack[stack_pos]) {
                case 0:
                    if (new_key == 20) return 2;
                    break;
                case 3:
                    if (new_key == 23) return 4;
                    else if(new_key == 29) return 5;
                    break;
                case 8:
                    if (new_key == 0)return 9;
                    else if (new_key == 16) return 10;
                    else if (new_key == 17) return 11;
                    else if (new_key == 20) return 12;
                    else if (new_key == 27) return 13;
                    else if (new_key == 28) return 14;
                    else if (new_key == 29) return 15;
                    break;
                case 17:
                    if (new_key == 20) return 19;
                    break;
            }
        }
        else {
        	/*
     		38, 30, 39 // �� �� ��
    		38, 31, 40 // �� �� ��
    		38, 50, 41 //�� �� ��
    		43, 34, 44 // �� �� ��
    		43, 35, 45 // �� �� ��
    		43, 50, 46 // �� �� ��
    		48, 50, 49 // �� �� ��
*/
            switch (mHangulKeyStack[stack_pos]) {
                case 38:
                    if (new_key == 30) return 39;
                    else if (new_key == 31) return 40;
                    else if (new_key == 50) return 41;
                    break;
                case 43:
                    if (new_key == 34) return 44;
                    else if (new_key == 35) return 45;
                    else if (new_key == 50) return 46;
                    break;
                case 48:
                    if (new_key == 50) return 49;
                    break;
            }
        }
        return 0;
    }



    private void hangulSendKey(int newHangulChar, int hCursor) {

        if (hCursor == HCURSOR_NEW) {
            Log.i("Hangul", "HCURSOR_NEW");

            getComposing().append((char)newHangulChar);
            getCurrentInputConnection().setComposingText(getComposing(), 1);
            mHCursorState = HCURSOR_NEW;
        }
        else if (hCursor == HCURSOR_ADD) {
            mHCursorState = HCURSOR_ADD;
            Log.i("Hangul", "HCURSOR_ADD");
            if (getComposing().length() > 0) {
                getComposing().setLength(0);
                getCurrentInputConnection().finishComposingText();
            }

            getComposing().append((char)newHangulChar);
            getCurrentInputConnection().setComposingText(getComposing(), 1);
        }
        else if (hCursor == HCURSOR_UPDATE) {
            Log.i("Hangul", "HCURSOR_UPDATE");
            getComposing().setCharAt(0, (char)newHangulChar);
            getCurrentInputConnection().setComposingText(getComposing(), 1);
            mHCursorState = HCURSOR_UPDATE;
        }
        else if (hCursor == HCURSOR_APPEND) {
            Log.i("Hangul", "HCURSOR_APPEND");
            getComposing().append((char)newHangulChar);
            getCurrentInputConnection().setComposingText(getComposing(), 1);
            mHCursorState = HCURSOR_APPEND;
        }
        else if (hCursor == HCURSOR_NONE) {
            if (newHangulChar == -1) {
                Log.i("Hangul", "HCURSOR_NONE [DEL -1]");
                keyDownUp(KeyEvent.KEYCODE_DEL);
                clearHangul();
            }
            else if (newHangulChar == -2) {
                int hangulKeyIdx;
                int cho_idx,jung_idx,jong_idx;

                Log.i("Hangul", "HCURSOR_NONE [DEL -2]");

                switch(mHangulState) {
                    case H_STATE_0:
                        keyDownUp(KeyEvent.KEYCODE_DEL);
                        break;
                    case H_STATE_1: // �ʼ�
//					keyDownUp(KeyEvent.KEYCODE_DEL);
                        getComposing().setLength(0);
                        getCurrentInputConnection().commitText("", 0);
                        clearHangul();
                        mHangulState = H_STATE_0;
                        break;
                    case H_STATE_2: // �ʼ�(������)
                        newHangulChar = 0x3131 + mHangulKeyStack[0];
                        hangulSendKey(newHangulChar, HCURSOR_UPDATE);
                        mHangulKeyStack[1] = 0;
                        mHangulJamoStack[0] = mHangulKeyStack[0];
                        mHangulState = H_STATE_1; // goto �ʼ�
                        break;
                    case H_STATE_3: // �߼�(�ܸ���,������)
                        if (mHangulKeyStack[3] == 0) {
//						keyDownUp(KeyEvent.KEYCODE_DEL);
                            getComposing().setLength(0);
                            getCurrentInputConnection().commitText("", 0);
                            clearHangul();
                            mHangulState = H_STATE_0;
                        }
                        else {
                            mHangulKeyStack[3] = 0;
                            newHangulChar = 0x314F + (mHangulKeyStack[2] - 30);
                            hangulSendKey(newHangulChar, HCURSOR_UPDATE);
                            mHangulJamoStack[1] = mHangulKeyStack[2];
                            mHangulState = H_STATE_3; // goto �߼�
                        }
                        break;
                    case H_STATE_4: // �ʼ�,�߼�(�ܸ���,������)
                        if (mHangulKeyStack[3] == 0) {
                            mHangulKeyStack[2] = 0;
                            mHangulJamoStack[1] = 0;
                            newHangulChar = 0x3131 + mHangulJamoStack[0];
                            hangulSendKey(newHangulChar, HCURSOR_UPDATE);
                            mHangulState = H_STATE_1; // goto �ʼ�
                        }
                        else {
                            mHangulJamoStack[1]= mHangulKeyStack[2];
                            mHangulKeyStack[3] = 0;
                            cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                            jung_idx = mHangulJamoStack[1] - 30;
                            jong_idx = h_jongsung_idx[mHangulJamoStack[2]];
                            newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                            hangulSendKey(newHangulChar, HCURSOR_UPDATE);
                        }
                        break;
                    case H_STATE_5:	// �ʼ�,�߼�,����
                        mHangulJamoStack[2] = 0;
                        mHangulKeyStack[4] = 0;
                        cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                        jung_idx = mHangulJamoStack[1] - 30;
                        jong_idx = h_jongsung_idx[mHangulJamoStack[2]];
                        newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                        hangulSendKey(newHangulChar, HCURSOR_UPDATE);
                        mHangulState = H_STATE_4;
                        break;
                    case H_STATE_6:
                        mHangulKeyStack[5] = 0;
                        mHangulJamoStack[2] = mHangulKeyStack[4];
                        cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                        jung_idx = mHangulJamoStack[1] - 30;
                        jong_idx = h_jongsung_idx[mHangulJamoStack[2]+1];;
                        newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                        hangulSendKey(newHangulChar,HCURSOR_UPDATE);
                        mHangulState = H_STATE_5;
                        break;
                }
            }
            else if (newHangulChar == -3) {
                Log.i("Hangul", "HCURSOR_NONE [DEL -3]");
                final int length = getComposing().length();
                if (length > 1) {
                    getComposing().delete(length - 1, length);
                }
            }

        }
    }

    private void sendSejongKey(char newHangulChar, char hCursor) {

        Log.i("Hangul", "newHangulChar[" + Integer.toString(newHangulChar) + "]");

        if (hCursor == HCURSOR_NEW) {
            Log.i("Hangul", "HCURSOR_NEW");

            getComposing().append(newHangulChar);
            getCurrentInputConnection().setComposingText(getComposing(), 1);
            mHCursorState = HCURSOR_NEW;
        }
        else if (hCursor == HCURSOR_ADD) {
            mHCursorState = HCURSOR_ADD;
            Log.i("Hangul", "HCURSOR_ADD");
            if (getComposing().length() > 0) {
                getComposing().setLength(0);
                getCurrentInputConnection().finishComposingText();
            }

            getComposing().append(newHangulChar);
            getCurrentInputConnection().setComposingText(getComposing(), 1);
        }
        else if (hCursor == HCURSOR_UPDATE) {
            Log.i("Hangul", "HCURSOR_UPDATE");
            getComposing().setCharAt(0, newHangulChar);
            getCurrentInputConnection().setComposingText(getComposing(), 1);
            mHCursorState = HCURSOR_UPDATE;
        }
        else if (hCursor == HCURSOR_APPEND) {
            Log.i("Hangul", "HCURSOR_APPEND");
            getComposing().append(newHangulChar);
            getCurrentInputConnection().setComposingText(getComposing(), 1);
            mHCursorState = HCURSOR_APPEND;
        }
        else if (hCursor == HCURSOR_UPDATE_LAST) {
            Log.i("Hangul", "HCURSOR_UPDATE_LAST");
            getComposing().setCharAt(1, newHangulChar);
            getCurrentInputConnection().setComposingText(getComposing(), 1);
            mHCursorState = HCURSOR_UPDATE_LAST;
        }
        else if (hCursor == HCURSOR_DELETE_LAST) {
            Log.i("Hangul", "HCURSOR_DELETE_LAST");
            final int length = getComposing().length();
            if (length > 1) {
                Log.i("Hangul", "Delete start :" + Integer.toString(length));
                getComposing().delete(length - 1, length);
                getCurrentInputConnection().setComposingText(getComposing(), 1);
            }
        }
        else if (hCursor == HCURSOR_DELETE) {
            char hChar;
            char cho_idx, jung_idx, jong_idx;
            switch(ko_state_idx) {
                case KO_S_0000:
                case KO_S_0100:
                case KO_S_1000:
                    keyDownUp(KeyEvent.KEYCODE_DEL);
                    clearHangul();
                    break;
                case KO_S_1100:
                    ko_state_middle_idx = jungsung_stack[ko_state_middle_idx - 1];
                    if (ko_state_middle_idx > 0) {
                        cho_idx = h_chosung_idx[chosung_code[ko_state_first_idx - 1]];
                        jung_idx = (char)(ko_state_middle_idx - 3);
                        hChar = (char)(0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28)));
                        sendSejongKey(hChar, HCURSOR_UPDATE);
                    }
                    else {
                        newHangulChar = (char)(0x3131 + chosung_code[ko_state_first_idx - 1]);
                        sendSejongKey(newHangulChar, HCURSOR_UPDATE);
                        ko_state_idx = KO_S_1000;
                    }
                    break;
                case KO_S_1110:
                    cho_idx = h_chosung_idx[chosung_code[ko_state_first_idx - 1]];
                    jung_idx = (char)(ko_state_middle_idx - 3);
                    hChar = (char)(0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28)));
                    sendSejongKey(hChar, HCURSOR_UPDATE);
                    ko_state_last_idx = 0;
                    ko_state_idx = KO_S_1110;
                    break;
                case KO_S_1111:
                    ko_state_last_idx = jongsung_28idx[ko_state_last_idx - 28];
                    sendSejongKey((char)0,HCURSOR_DELETE_LAST);
                    ko_state_next_idx = 0;
                    ko_state_idx = KO_S_1110;
                    break;
            }
        }

    }


    private void handleHangul(int primaryCode, int[] keyCodes) {

        int hangulKeyIdx = -1;
        int newHangulChar;
        int cho_idx,jung_idx,jong_idx;
        int hangulChar = 0;
/*
        if (mHangulCursorMoved == 1) {
        	clearHangul();
        	Log.i("Hangul", "clear Hangul at handleHangul by mHangulCursorMoved");
        	mHangulCursorMoved = 0;
        }
*/
        // Log.i("Hangul", "PrimaryCode[" + Integer.toString(primaryCode)+"]");

        if (primaryCode >= 0x61 && primaryCode <= 0x7A) {
            //            Log.i("SoftKey", "handleHangul - hancode");

            if (mHangulShiftState == 0) {
                hangulKeyIdx = e2h_map[primaryCode - 0x61];
            }
            else {
                hangulKeyIdx = e2h_map[primaryCode - 0x61 + 26];
//                Keyboard currentKeyboard = mInputView.getKeyboard();
                mHangulShiftedKeyboard.setShifted(false);
                mKeyboardView.setKeyboard(mHangulKeyboard);
                mHangulKeyboard.setShifted(false);
                mHangulShiftState = 0;
            }
            hangulChar = 1;
        }
        else if (primaryCode >= 0x41 && primaryCode <= 0x5A) {
            hangulKeyIdx = e2h_map[primaryCode - 0x41 + 26];
            hangulChar = 1;
        }
        /*
        else  if (primaryCode >= 0x3131 && primaryCode <= 0x3163) {
        	hangulKeyIdx = primaryCode - 0x3131;
        	hangulChar = 1;
        }
        */
        else {
            hangulChar = 0;
        }


        if (hangulChar == 1) {

            switch(mHangulState) {

                case H_STATE_0: // Hangul Clear State
                    // Log.i("SoftKey", "HAN_STATE 0");
                    if (hangulKeyIdx < 30) { // if ����
                        newHangulChar = 0x3131 + hangulKeyIdx;
                        hangulSendKey(newHangulChar, HCURSOR_NEW);
                        mHangulKeyStack[0] = hangulKeyIdx;
                        mHangulJamoStack[0] = hangulKeyIdx;
                        mHangulState = H_STATE_1; // goto �ʼ�
                    }
                    else { // if ����
                        newHangulChar = 0x314F + (hangulKeyIdx - 30);
                        hangulSendKey(newHangulChar, HCURSOR_NEW);
                        mHangulKeyStack[2] = hangulKeyIdx;
                        mHangulJamoStack[1] = hangulKeyIdx;
                        mHangulState = H_STATE_3; // goto �߼�
                    }
                    break;

                case H_STATE_1: // �ʼ�
                    // Log.i("SoftKey", "HAN_STATE 1");
                    if (hangulKeyIdx < 30) { // if ����
                        int newHangulKeyIdx = isHangulKey(0,hangulKeyIdx);
                        if (newHangulKeyIdx > 0) { // if ������
                            newHangulChar = 0x3131 + newHangulKeyIdx;
                            mHangulKeyStack[1] = hangulKeyIdx;
                            mHangulJamoStack[0] = newHangulKeyIdx;
//	                    hangulSendKey(-1);
                            hangulSendKey(newHangulChar, HCURSOR_UPDATE);
                            mHangulState = H_STATE_2; // goto �ʼ�(������)
                        }
                        else { // if ����

                            // cursor error trick start
                            newHangulChar = 0x3131 + mHangulJamoStack[0];
                            hangulSendKey(newHangulChar,HCURSOR_UPDATE);
                            // trick end

                            newHangulChar = 0x3131 + hangulKeyIdx;
                            hangulSendKey(newHangulChar, HCURSOR_ADD);
                            mHangulKeyStack[0] = hangulKeyIdx;
                            mHangulJamoStack[0] = hangulKeyIdx;
                            mHangulState = H_STATE_1; // goto �ʼ�
                        }
                    }
                    else { // if ����
                        mHangulKeyStack[2] = hangulKeyIdx;
                        mHangulJamoStack[1] = hangulKeyIdx;
//	                hangulSendKey(-1);
                        cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                        jung_idx = mHangulJamoStack[1] - 30;
                        jong_idx = h_jongsung_idx[mHangulJamoStack[2]];
                        newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                        hangulSendKey(newHangulChar, HCURSOR_UPDATE);
                        mHangulState = H_STATE_4; // goto �ʼ�,�߼�
                    }
                    break;

                case H_STATE_2: // �ʼ�(������)
                    // Log.i("SoftKey", "HAN_STATE 2");
                    if (hangulKeyIdx < 30) { // if ����

                        // cursor error trick start
                        newHangulChar = 0x3131 + mHangulJamoStack[0];
                        hangulSendKey(newHangulChar,HCURSOR_UPDATE);
                        // trick end


                        mHangulKeyStack[0] = hangulKeyIdx;
                        mHangulJamoStack[0] = hangulKeyIdx;
                        mHangulJamoStack[1] = 0;
                        newHangulChar = 0x3131 + hangulKeyIdx;
                        hangulSendKey(newHangulChar, HCURSOR_ADD);
                        mHangulState = H_STATE_1; // goto �ʼ�
                    }
                    else { // if ����
                        newHangulChar = 0x3131 + mHangulKeyStack[0];
                        mHangulKeyStack[0] = mHangulKeyStack[1];
                        mHangulJamoStack[0] = mHangulKeyStack[0];
                        mHangulKeyStack[1] = 0;
//	                hangulSendKey(-1);
                        hangulSendKey(newHangulChar, HCURSOR_UPDATE);

                        mHangulKeyStack[2] = hangulKeyIdx;
                        mHangulJamoStack[1] = mHangulKeyStack[2];
                        cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                        jung_idx = mHangulJamoStack[1] - 30;
                        jong_idx = 0;

                        newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                        hangulSendKey(newHangulChar, HCURSOR_ADD);
                        mHangulState = H_STATE_4; // goto �ʼ�,�߼�
                    }
                    break;

                case H_STATE_3: // �߼�(�ܸ���,������)
                    // Log.i("SoftKey", "HAN_STATE 3");
                    if (hangulKeyIdx < 30) { // ����

                        // cursor error trick start
                        newHangulChar = 0x314F + (mHangulJamoStack[1] - 30);
                        hangulSendKey(newHangulChar, HCURSOR_UPDATE);
                        // trick end

                        newHangulChar = 0x3131 + hangulKeyIdx;
                        hangulSendKey(newHangulChar, HCURSOR_ADD);
                        mHangulKeyStack[0] = hangulKeyIdx;
                        mHangulJamoStack[0] = hangulKeyIdx;
                        mHangulJamoStack[1] = 0;
                        mHangulState = H_STATE_1; // goto �ʼ�
                    }
                    else { // ����
                        if (mHangulKeyStack[3] == 0) {
                            int newHangulKeyIdx = isHangulKey(2,hangulKeyIdx);
                            if (newHangulKeyIdx > 0) { // ������
                                //	                	hangulSendKey(-1);
                                newHangulChar = 0x314F + (newHangulKeyIdx - 30);
                                hangulSendKey(newHangulChar, HCURSOR_UPDATE);
                                mHangulKeyStack[3] = hangulKeyIdx;
                                mHangulJamoStack[1] = newHangulKeyIdx;
                            }
                            else { // ����

                                // cursor error trick start
                                newHangulChar = 0x314F + (mHangulJamoStack[1] - 30);
                                hangulSendKey(newHangulChar, HCURSOR_UPDATE);
                                // trick end

                                newHangulChar = 0x314F + (hangulKeyIdx - 30);
                                hangulSendKey(newHangulChar,HCURSOR_ADD);
                                mHangulKeyStack[2] = hangulKeyIdx;
                                mHangulJamoStack[1] = hangulKeyIdx;
                            }
                        }
                        else {

                            // cursor error trick start
                            newHangulChar = 0x314F + (mHangulJamoStack[1] - 30);
                            hangulSendKey(newHangulChar, HCURSOR_UPDATE);
                            // trick end

                            newHangulChar = 0x314F + (hangulKeyIdx - 30);
                            hangulSendKey(newHangulChar,HCURSOR_ADD);
                            mHangulKeyStack[2] = hangulKeyIdx;
                            mHangulJamoStack[1] = hangulKeyIdx;
                            mHangulKeyStack[3] = 0;
                        }
                        mHangulState = H_STATE_3;
                    }
                    break;
                case H_STATE_4: // �ʼ�,�߼�(�ܸ���,������)
                    // Log.i("SoftKey", "HAN_STATE 4");
                    if (hangulKeyIdx < 30) { // if ����
                        mHangulKeyStack[4] = hangulKeyIdx;
                        mHangulJamoStack[2] = hangulKeyIdx;
//	                hangulSendKey(-1);
                        cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                        jung_idx = mHangulJamoStack[1] - 30;
                        jong_idx = h_jongsung_idx[mHangulJamoStack[2]+1];
                        newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                        hangulSendKey(newHangulChar, HCURSOR_UPDATE);
                        if (jong_idx == 0) { // if ���� is not valid ex, �� + ��
                            mHangulKeyStack[0] = hangulKeyIdx;
                            mHangulKeyStack[1] = 0;
                            mHangulKeyStack[2] = 0;
                            mHangulKeyStack[3] = 0;
                            mHangulKeyStack[4] = 0;
                            mHangulJamoStack[0] = hangulKeyIdx;
                            mHangulJamoStack[1] = 0;
                            mHangulJamoStack[2] = 0;
                            newHangulChar = 0x3131 + hangulKeyIdx;
                            hangulSendKey(newHangulChar,HCURSOR_ADD);
                            mHangulState = H_STATE_1; // goto �ʼ�
                        }
                        else {
                            mHangulState = H_STATE_5; // goto �ʼ�,�߼�,����
                        }
                    }
                    else { // if ����
                        if (mHangulKeyStack[3] == 0) {
                            int newHangulKeyIdx = isHangulKey(2,hangulKeyIdx);
                            if (newHangulKeyIdx > 0) { // if ������
                                //	                	hangulSendKey(-1);
                                //	                    mHangulKeyStack[2] = newHangulKeyIdx;
                                mHangulKeyStack[3] = hangulKeyIdx;
                                mHangulJamoStack[1] = newHangulKeyIdx;
                                cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                                jung_idx = mHangulJamoStack[1] - 30;
                                jong_idx = 0;
                                newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                                hangulSendKey(newHangulChar,HCURSOR_UPDATE);
                                mHangulState = H_STATE_4; // goto �ʼ�,�߼�
                            }
                            else { // if invalid ������

                                // cursor error trick start
                                cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                                jung_idx = mHangulJamoStack[1] - 30;
                                jong_idx = 0;
                                newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                                hangulSendKey(newHangulChar,HCURSOR_UPDATE);
                                // trick end

                                newHangulChar = 0x314F + (hangulKeyIdx - 30);
                                hangulSendKey(newHangulChar,HCURSOR_ADD);
                                mHangulKeyStack[0] = 0;
                                mHangulKeyStack[1] = 0;
                                mHangulJamoStack[0] = 0;
                                mHangulKeyStack[2] = hangulKeyIdx;
                                mHangulJamoStack[1] = hangulKeyIdx;
                                mHangulState = H_STATE_3; // goto �߼�
                            }
                        }
                        else {

                            // cursor error trick start
                            cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                            jung_idx = mHangulJamoStack[1] - 30;
                            jong_idx = 0;
                            newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                            hangulSendKey(newHangulChar,HCURSOR_UPDATE);
                            // trick end


                            newHangulChar = 0x314F + (hangulKeyIdx - 30);
                            hangulSendKey(newHangulChar,HCURSOR_ADD);
                            mHangulKeyStack[0] = 0;
                            mHangulKeyStack[1] = 0;
                            mHangulJamoStack[0] = 0;
                            mHangulKeyStack[2] = hangulKeyIdx;
                            mHangulJamoStack[1] = hangulKeyIdx;
                            mHangulKeyStack[3] = 0;
                            mHangulState = H_STATE_3; // goto �߼�

                        }
                    }
                    break;
                case H_STATE_5: // �ʼ�,�߼�,����
                    // Log.i("SoftKey", "HAN_STATE 5");
                    if (hangulKeyIdx < 30) { // if ����
                        int newHangulKeyIdx = isHangulKey(4,hangulKeyIdx);
                        if (newHangulKeyIdx > 0) { // if ���� == ������
//	                	hangulSendKey(-1);
                            mHangulKeyStack[5] = hangulKeyIdx;
                            mHangulJamoStack[2] = newHangulKeyIdx;

                            cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                            jung_idx = mHangulJamoStack[1] - 30;
                            jong_idx = h_jongsung_idx[mHangulJamoStack[2]+1];;
                            newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                            hangulSendKey(newHangulChar,HCURSOR_UPDATE);
                            mHangulState = H_STATE_6; // goto  �ʼ�,�߼�,����(������)
                        }
                        else { // if ���� != ������

                            // cursor error trick start
                            cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                            jung_idx = mHangulJamoStack[1] - 30;
                            jong_idx = h_jongsung_idx[mHangulJamoStack[2]+1];;
                            newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                            hangulSendKey(newHangulChar,HCURSOR_UPDATE);
                            // trick end


                            mHangulKeyStack[0] = hangulKeyIdx;
                            mHangulKeyStack[1] = 0;
                            mHangulKeyStack[2] = 0;
                            mHangulKeyStack[3] = 0;
                            mHangulKeyStack[4] = 0;
                            mHangulJamoStack[0] = hangulKeyIdx;
                            mHangulJamoStack[1] = 0;
                            mHangulJamoStack[2] = 0;
                            newHangulChar = 0x3131 + hangulKeyIdx;
                            hangulSendKey(newHangulChar,HCURSOR_ADD);
                            mHangulState = H_STATE_1; // goto �ʼ�
                        }
                    }
                    else { // if ����
//	            	hangulSendKey(-1);

                        cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                        jung_idx = mHangulJamoStack[1] - 30;
                        jong_idx = 0;
                        newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                        hangulSendKey(newHangulChar, HCURSOR_UPDATE);

                        mHangulKeyStack[0] = mHangulKeyStack[4];
                        mHangulKeyStack[1] = 0;
                        mHangulKeyStack[2] = hangulKeyIdx;
                        mHangulKeyStack[3] = 0;
                        mHangulKeyStack[4] = 0;
                        mHangulJamoStack[0] = mHangulKeyStack[0];
                        mHangulJamoStack[1] = mHangulKeyStack[2];
                        mHangulJamoStack[2] = 0;

                        cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                        jung_idx = mHangulJamoStack[1] - 30;
                        jong_idx = 0;
                        newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                        hangulSendKey(newHangulChar, HCURSOR_ADD);

                        // Log.i("SoftKey", "--- Goto HAN_STATE 4");
                        mHangulState = H_STATE_4; // goto �ʼ�,�߼�
                    }
                    break;
                case H_STATE_6: // �ʼ�,�߼�,����(������)
                    // Log.i("SoftKey", "HAN_STATE 6");
                    if (hangulKeyIdx < 30) { // if ����

                        // cursor error trick start
                        cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                        jung_idx = mHangulJamoStack[1] - 30;
                        jong_idx = h_jongsung_idx[mHangulJamoStack[2]+1];;
                        newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                        hangulSendKey(newHangulChar,HCURSOR_UPDATE);
                        // trick end


                        mHangulKeyStack[0] = hangulKeyIdx;
                        mHangulKeyStack[1] = 0;
                        mHangulKeyStack[2] = 0;
                        mHangulKeyStack[3] = 0;
                        mHangulKeyStack[4] = 0;
                        mHangulJamoStack[0] = hangulKeyIdx;
                        mHangulJamoStack[1] = 0;
                        mHangulJamoStack[2] = 0;

                        newHangulChar = 0x3131 + hangulKeyIdx;
                        hangulSendKey(newHangulChar,HCURSOR_ADD);

                        mHangulState = H_STATE_1; // goto �ʼ�
                    }
                    else { // if ����
//	            	hangulSendKey(-1);
                        mHangulJamoStack[2] = mHangulKeyStack[4];

                        cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                        jung_idx = mHangulJamoStack[1] - 30;
                        jong_idx = h_jongsung_idx[mHangulJamoStack[2]+1];;
                        newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                        hangulSendKey(newHangulChar, HCURSOR_UPDATE);

                        mHangulKeyStack[0] = mHangulKeyStack[5];
                        mHangulKeyStack[1] = 0;
                        mHangulKeyStack[2] = hangulKeyIdx;
                        mHangulKeyStack[3] = 0;
                        mHangulKeyStack[4] = 0;
                        mHangulKeyStack[5] = 0;
                        mHangulJamoStack[0] = mHangulKeyStack[0];
                        mHangulJamoStack[1] = mHangulKeyStack[2];
                        mHangulJamoStack[2] = 0;

                        cho_idx = h_chosung_idx[mHangulJamoStack[0]];
                        jung_idx = mHangulJamoStack[1] - 30;
                        jong_idx = 0;
                        newHangulChar = 0xAC00 + ((cho_idx * 21 * 28) + (jung_idx * 28) + jong_idx);
                        hangulSendKey(newHangulChar,HCURSOR_ADD);

                        mHangulState = H_STATE_4; // goto �ʼ�,�߼�
                    }
                    break;
            }
        }
        else {
            // Log.i("Hangul", "handleHangul - No hancode");
            clearHangul();
            sendKey(primaryCode);
        }

    }

    private void clearSejong() {
        ko_state_idx = KO_S_0000;
        ko_state_first_idx = 0;
        ko_state_middle_idx = 0;
        ko_state_last_idx = 0;
        ko_state_next_idx = 0;
        prev_key = -1;
        return;
    }
}
