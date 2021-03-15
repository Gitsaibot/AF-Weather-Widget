package net.veierland.aix;

import android.app.Activity;
import android.os.Bundle;
import net.veierland.aix.databinding.IntroBinding;

public class AixIntro extends Activity {
	
	public final static String ACTION_SHOW_HELP = "aix.intent.action.SHOW_HELP";
	private IntroBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		binding = IntroBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

	}
	
}
