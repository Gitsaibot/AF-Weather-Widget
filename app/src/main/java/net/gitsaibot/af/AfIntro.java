package net.gitsaibot.af;

import android.app.Activity;
import android.os.Bundle;
import net.gitsaibot.af.databinding.IntroBinding;

public class AfIntro extends Activity {
	
	private IntroBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		binding = IntroBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

	}
	
}
