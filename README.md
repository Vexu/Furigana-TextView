# FuriganaView
FuriganaView is a a view for Android that can add furigana on top of kanji. Text can be bolded, italicized and underlined.


![Example](https://github.com/Vexu/Furigana-TextView/blob/master/screenshot.png)

## Implementation
By default furigana is set as "{漢字;かんじ}" and bold, italic and underline by their html tags.

### java
```Java
    FuriganaView furiganaView = new FuriganaView(this);
    furiganaView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER)
    furiganaView.setFuriganaSize(10f);
    furiganaView.setTextSize(20f);
    furiganaView.setLineSpacing(10f);
    furiganaView.setText(TEXT);
```

### xml
```xml
    <FuriganaView
        android:maxLines="2"
        android:text="TEXT"
        android:textAlignment="center"
        android:lineSpacingExtra="4sp"
        android:textSize="40sp"
        custom:furiganaSize="15sp"/>
```