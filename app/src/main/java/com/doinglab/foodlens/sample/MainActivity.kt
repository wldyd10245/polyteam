package com.doinglab.foodlens.sample

import android.net.Uri
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.doinglab.foodlens.sample.databinding.ActivityMainBinding
import com.doinglab.foodlens.sample.listview.RecognitionItem
import com.doinglab.foodlens.sample.listview.RecognitionListAdapter
import com.doinglab.foodlens.sample.util.BitmapUtil
import com.doinglab.foodlens.sdk.core.FoodLensCore
import com.doinglab.foodlens.sdk.core.RecognitionResultHandler
import com.doinglab.foodlens.sdk.core.error.BaseError
import com.doinglab.foodlens.sdk.core.model.result.RecognitionResult
import com.doinglab.foodlens.sdk.core.type.*
import com.doinglab.foodlens.sdk.ui.FoodLensUI
import com.doinglab.foodlens.sdk.ui.UIServiceResultHandler
import com.doinglab.foodlens.sdk.ui.config.FoodLensSettingConfig
import com.doinglab.foodlens.sdk.ui.config.FoodLensUiConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import com.doinglab.foodlens.sample.db.FoodDatabase
import com.doinglab.foodlens.sample.db.entity.FoodEntity
import com.doinglab.foodlens.sample.db.repository.FoodRepository
import com.doinglab.foodlens.sample.db.repository.FoodViewModel
import com.doinglab.foodlens.sample.db.repository.FoodViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class MainActivity : AppCompatActivity() {

    // GoogleSignInClient를 선언합니다.
    private lateinit var googleSignInClient: GoogleSignInClient

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val foodLensCoreService by lazy {
        FoodLensCore.createFoodLensService(this, FoodLensType.FoodLens)
    }

    private val foodLensUiService by lazy {
        FoodLensUI.createFoodLensService(this, FoodLensType.FoodLens)
    }

    private val listAdapter by lazy {
        RecognitionListAdapter()
    }

    private var recognitionResult:RecognitionResult? = null
    private var foodImagePath = ""

    private lateinit var viewModel: FoodViewModel
    private lateinit var repository: FoodRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.list.adapter = listAdapter

        // LoginActivity에서 전달된 ID 토큰 받기
        val idToken = intent.getStringExtra("idToken")

        // Google Sign-In 옵션 설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // 웹 클라이언트 ID
            .requestEmail() // 이메일 요청
            .build()

        // GoogleSignInClient 초기화
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 로그아웃 버튼 클릭 리스너 설정
        val logoutButton: Button = findViewById(R.id.button_logout)
        logoutButton.setOnClickListener {
            logout()
        }

        binding.btnRunUiCamera.setOnClickListener {
            foodLensUiService.startFoodLensCamera(this, foodLensActivityResult, object :
                UIServiceResultHandler {
                override fun onSuccess(result: RecognitionResult?) {
                    result?.let {
                        setRecognitionResultData(result)
                    }
                }

                override fun onError(errorReason: BaseError?) {
                    Toast.makeText(this@MainActivity, errorReason?.getMessage(), Toast.LENGTH_SHORT).show()
                    Log.d("foodLens", "foodLensCameraResult onError ${errorReason?.getMessage()}")
                }

                override fun onCancel() {
                    Log.d("foodLens", "foodLensCameraResult cancel")
                }
            })
        }

        val button: Button = findViewById(R.id.button_open_website)
        button.setOnClickListener {
            // 웹사이트로 이동하는 코드
            val intent = Intent(this, WebViewActivity::class.java)
            intent.putExtra("idToken", idToken) // ID 토큰을 WebViewActivity로 전달
            startActivity(intent)
        }

        // SNS 공유 버튼 설정
        binding.shareButton.setOnClickListener {
            shareFoodInfo()
        }

        // Repository 초기화
        val dao = FoodDatabase.getInstance(application).foodDao()
        repository = FoodRepository(dao)

        // ViewModelFactory 초기화
        val factory = FoodViewModelFactory(application, repository)
        viewModel = ViewModelProvider(this, factory)[FoodViewModel::class.java]



        binding.viewDietButton.setOnClickListener {
            // "식단 모아보기" 버튼 클릭 시 새로운 액티비티로 이동
            val intent = Intent(this, DietViewActivity::class.java)
            startActivity(intent)
        }

    }

    // 로그아웃 기능
    private fun logout() {
        // Google Sign-Out
        googleSignInClient.signOut().addOnCompleteListener {
            // SharedPreferences에서 토큰 삭제
            val sharedPreferences: SharedPreferences = getSharedPreferences("YourAppPrefs", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.remove("access_token") // 액세스 토큰 삭제
            editor.apply()
            Log.d("MainActivity", "User logged out")
            // 로그인 화면으로 이동
            finish() // 현재 액티비티 종료
            startActivity(Intent(this, LoginActivity::class.java)) // 로그인 액티비티로 이동
        }
    }
    // SNS 공유 기능
    private fun shareFoodInfo() {
        val items: List<RecognitionItem> = listAdapter.currentList

        if (items.isNotEmpty()) {
            val firstItem = items[0]

            // 음식 정보 텍스트 구성
            val shareText = """
                Food Information:
                Name: ${firstItem.name}
                ${firstItem.foodNutrition}
                ${firstItem.energy}
            """.trimIndent()

            // 음식 이미지 Bitmap 가져오기
            val bitmapDrawable = firstItem.icon as BitmapDrawable
            val originalBitmap = bitmapDrawable.bitmap

            // 텍스트가 포함된 새로운 Bitmap 생성
            val bitmapWithText = createImageWithText(originalBitmap, shareText)

            // 음식 정보와 이미지를 공유
            shareFoodInfoWithImage(bitmapWithText, shareText)
        } else {
            Toast.makeText(this, "No food information to share.", Toast.LENGTH_SHORT).show()
        }
    }

    // 텍스트가 포함된 이미지 생성 함수 (자동으로 잘리지 않게 텍스트를 나누기)
    private fun createImageWithText(bitmap: Bitmap, text: String): Bitmap {
        // 복사된 비트맵 생성
        val canvasBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(canvasBitmap)

        // 텍스트 스타일 설정
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 40f
        }

        val maxTextWidth = canvas.width - 20f // 텍스트가 그려질 최대 너비 (이미지 좌우 여백을 고려)

        // 텍스트를 줄바꿈할 리스트 생성
        val textLines = getTextLines(text, paint, maxTextWidth)

        // 텍스트가 이미지 높이를 넘지 않도록 시작 위치 계산
        val textHeight = paint.textSize + 10f
        val totalTextHeight = textHeight * textLines.size
        var textY = canvas.height - totalTextHeight - 20f // 이미지 하단에서 일정 간격 위로 텍스트 위치

        // 여러 줄로 나뉜 텍스트를 한 줄씩 그리기
        for (line in textLines) {
            canvas.drawText(line, 10f, textY, paint)
            textY += textHeight // 다음 줄 Y 위치
        }

        return canvasBitmap
    }

    // 텍스트를 이미지 너비에 맞춰 여러 줄로 나누는 함수
    private fun getTextLines(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ") // 텍스트를 공백 기준으로 단어 분리
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            // 현재 줄에 단어를 추가한 후 너비 계산
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) < maxWidth) {
                currentLine = testLine // 텍스트가 너비를 넘지 않으면 현재 줄에 단어 추가
            } else {
                lines.add(currentLine) // 너비를 넘으면 현재 줄을 저장하고 다음 줄 시작
                currentLine = word
            }
        }

        // 마지막 줄도 추가
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }

    private fun shareFoodInfoWithImage(bitmap: Bitmap, shareText: String) {
        val imageUri = getImageUri(this, bitmap)

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_STREAM, imageUri)
            type = "image/png"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share Food Info via"))
    }

    // 이미지를 임시 파일로 저장하고 URI를 반환하는 함수
    private fun getImageUri(context: Context, bitmap: Bitmap): Uri? {
        val imagesFolder = File(context.getExternalFilesDir(null), "shared_images")
        var uri: Uri? = null
        try {
            imagesFolder.mkdirs()
            val file = File(imagesFolder, "shared_image.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
            stream.flush()
            stream.close()
            // FileProvider를 사용해 content:// URI 생성
            uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return uri
    }

    private fun startFoodLensCore(byteData: ByteArray) {
        foodLensCoreService.predict(byteData, object : RecognitionResultHandler {
            override fun onSuccess(result: RecognitionResult?) {
                result?.let {
                    setRecognitionResultData(result)
                }
            }

            override fun onError(errorReason: BaseError?) {
                Toast.makeText(this@MainActivity, errorReason?.getMessage(), Toast.LENGTH_SHORT).show()
            }
        })
    }



    private var foodLensActivityResult: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            foodLensUiService.onActivityResult(result.resultCode, result.data)
        }


    private fun setRecognitionResultData(resultData: RecognitionResult) {
        val mutableList = mutableListOf<RecognitionItem>()
        recognitionResult = resultData

        //ui mode only
        resultData.imagePath?.let { imagePath->
            if(imagePath.isNotEmpty())
                foodImagePath = imagePath
        }

        val originBitmap = BitmapUtil.getBitmapFromFile(foodImagePath)

        resultData.foods.forEach { foodInfo ->
            foodInfo.let {
                val xMin = it.position?.xmin ?: 0
                val yMin = it.position?.ymin ?: 0
                val xMax = it.position?.xmax ?: originBitmap?.width ?: 0
                val yMax = it.position?.ymax ?: originBitmap?.height ?: 0
                val bitmap = BitmapUtil.cropBitmap(originBitmap, xMin, yMin, xMax, yMax)

                var carbohydrate = -1.0
                var protein = -1.0
                var fat = -1.0
                var unit = ""
                var energy = 0.0

                it.userSelected?.let { userSelected->
                    carbohydrate = userSelected.carbohydrate
                    protein = userSelected.protein
                    fat = userSelected.fat
                    unit = userSelected.unit?:""
                    energy = userSelected.energy
                }?:run {
                    it.candidates?.let { candidates ->
                        if(candidates.isNotEmpty()) {
                            carbohydrate = candidates[0].carbohydrate
                            protein = candidates[0].protein
                            fat = candidates[0].fat
                            unit = candidates[0].unit?:""
                            energy = candidates[0].energy
                        }
                    }
                }

                mutableList.add(
                    RecognitionItem(
                        name = "${getString(R.string.name)} : ${it.fullName}",
                        icon = BitmapDrawable(resources, bitmap),
                        foodPosition = "${getString(R.string.food_position)} : ${xMin}, ${yMin}, ${xMax}, $yMax",
                        foodNutrition = "${getString(R.string.carbohydrate)} : ${carbohydrate}, " +
                                "${getString(R.string.protein)} : ${protein}, " +
                                "${getString(R.string.fat)} : ${fat}, ",
                        energy = "${getString(R.string.energy)} : ${energy}, ${getString(R.string.amount)} : ${it.eatAmount} Unit : $unit",
                    )
                )

            }
        }
        listAdapter.submitList(mutableList)
    }


    private fun setOptionFoodLensCore() {
        //foodLensCore option
        foodLensCoreService.setLanguage(LanguageConfig.KO)
        foodLensCoreService.setImageResizeOption(ImageResizeOption.QUALITY)
        foodLensCoreService.setNutritionRetrieveOption(NutritionRetrieveOption.TOP1_NUTRITION_ONLY)
    }

    private fun setOptionFoodLensUI() {
        //foodLensUI Theme
        val uiConfig = FoodLensUiConfig()
        uiConfig.mainColor = getColor(R.color.purple_500)
        uiConfig.mainTextColor = getColor(R.color.grey_100)
        foodLensUiService.setUiConfig(uiConfig)

        //foodLensUI Option
        val settingConfig = FoodLensSettingConfig()
        settingConfig.isEnableCameraOrientation = true
        settingConfig.isShowPhotoGalleryIcon = true
        settingConfig.isShowManualInputIcon = true
        settingConfig.isShowHelpIcon = true
        settingConfig.isSaveToGallery = false
        settingConfig.isUseEatDatePopup = true
        settingConfig.imageResize = ImageResizeOption.NORMAL
        settingConfig.languageConfig = LanguageConfig.DEVICE
        settingConfig.eatDate = Date()
        settingConfig.mealType = MealType.afternoon_snack
        settingConfig.recommendedKcal = 2000f
        foodLensUiService.setSettingConfig(settingConfig)
    }


}