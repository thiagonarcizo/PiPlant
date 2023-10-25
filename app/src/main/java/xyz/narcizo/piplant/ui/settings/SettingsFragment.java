package xyz.narcizo.piplant.ui.settings;

import static androidx.core.graphics.TypefaceCompatUtil.getTempFile;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import xyz.narcizo.piplant.R;
import xyz.narcizo.piplant.databinding.FragmentSettingsBinding;
import xyz.narcizo.piplant.ui.home.HomeFragment;

public class SettingsFragment extends Fragment {
    private final String nomeFoto = "planta";
    private Button b_add;
    private Button b_about;
    private Button b_edit;
    private Button b_delete;
    private Button b_trocar;
    private Boolean hasPhoto = false;
    private ImageView logo;
    private String m_API = "";
    private String prevAPI;
    private String currentAPI;
    private Vibrator myVib;


    //Método que solicita a escolha de foto pelo usuário:
    ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //SharedPreferences para salvar se há foto escolhida pelo usuário:
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        //Recebe e amazena permanentemente a URI da imagem escolhida pelo usuário
                        Uri image_uri = result.getData().getData();
                        getActivity().getContentResolver().takePersistableUriPermission(image_uri, (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
                        try {
                            //Começa o processo de salvar a URI da imagem escolhida pelo usuário utilizando o FOS
                            FileOutputStream fos = getActivity().openFileOutput(nomeFoto, Context.MODE_PRIVATE);
                            fos.write(image_uri.toString().getBytes());
                            fos.close();
                            System.out.println(image_uri);
                            //Altera a exibição dos botões após a escolha da foto
                            b_add.setVisibility(View.GONE);
                            b_edit.setVisibility(View.VISIBLE);
                            b_delete.setVisibility(View.VISIBLE);
                            //Salva a informação de que HÁ foto escolhida pelo usuário
                            Boolean statusHasPhoto = prefs.edit().putBoolean("hasPhoto", true).commit();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prevAPI = prefs.getString("previousAPI", "");
        currentAPI = prefs.getString("API", "");
        System.out.println("prev api: "+prevAPI);
        System.out.println("api atual: "+currentAPI);


        View rootView = inflater.inflate(R.layout.fragment_settings, container, false);
        b_add = (Button) rootView.findViewById(R.id.b_adicionar);
        b_about = (Button) rootView.findViewById(R.id.b_sobre);
        b_edit = (Button) rootView.findViewById(R.id.b_editar);
        b_delete = (Button) rootView.findViewById(R.id.b_remover);
        logo = (ImageView) rootView.findViewById(R.id.logo);
        b_trocar = (Button) rootView.findViewById(R.id.b_trocar);

        myVib = (Vibrator) getContext().getSystemService(Vibrator.class);

        //Verifica se está no modo noturno do celular para escolher o logo correto (branco ou preto):
        int nightModeFlags =
                getContext().getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK;
        switch (nightModeFlags) {
            case Configuration.UI_MODE_NIGHT_YES:
                logo.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.piplantcomplete_b));
                break;

            case Configuration.UI_MODE_NIGHT_NO:
                logo.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.piplantcomplete));
                break;

            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                logo.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.piplantcomplete));
                break;
        }


        //Existe a foto a ser exibida?
        hasPhoto = prefs.getBoolean("hasPhoto", false);

        //Os botões no menu de configurações a depender da existência da foto:
        if (hasPhoto) {
            b_add.setVisibility(View.GONE);
            b_edit.setVisibility(View.VISIBLE);
            b_delete.setVisibility(View.VISIBLE);
        } else {
            b_add.setVisibility(View.VISIBLE);
            b_edit.setVisibility(View.GONE);
            b_delete.setVisibility(View.GONE);
        }

        //O que o botão de trocar API faz:
        b_trocar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    myVib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                } else {
                    myVib.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
                }
                //Chamo um AlertDialog para confirmar a troca da API
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Alterar a API");
                final EditText input = new EditText(getContext());
                input.setLines(1);
                input.setMaxLines(7);
                input.setGravity(View.TEXT_ALIGNMENT_GRAVITY | View.TEXT_ALIGNMENT_TEXT_START);
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                builder.setView(input);

                builder.setMessage("Deseja trocar a API? Isso pode afetar o funcionamento do aplicativo.");

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        m_API = input.getText().toString();
                        if (m_API.equals("") || !m_API.contains(".") || !m_API.contains("http")) {
                            Toast.makeText(getActivity(), "API não alterada!", Toast.LENGTH_SHORT).show();
                        } else {
                            Boolean statusPreviousAPI = prefs.edit().putString("previousAPI", currentAPI).commit();
                            Boolean statusHasAPI = prefs.edit().putString("API", m_API).commit();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                myVib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                            } else {
                                myVib.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
                            }
                            Toast.makeText(getActivity(), "API alterada com sucesso!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.setNeutralButton("Anterior", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        String prev = prefs.getString("previousAPI", "");
                        if (prev.equals("")) {
                            Toast.makeText(getActivity(), "Não há API anterior!", Toast.LENGTH_SHORT).show();
                        } else {
                            Boolean statusPreviousAPI = prefs.edit().putString("previousAPI", currentAPI).commit();
                            Boolean statusHasAPI = prefs.edit().putString("API", prev).commit();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                myVib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                            } else {
                                myVib.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
                            }
                            Toast.makeText(getActivity(), "API resetada com sucesso!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });


                builder.show();
            }
        });


        //O que o botão de remover foto faz:
        b_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    myVib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                } else {
                    myVib.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
                }
                //Chamo um AlertDialog para confirmar a remoção da foto
                AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
                alertDialog.setMessage("Deseja remover a foto? Ela não será apagada da galeria.");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Sim",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    //A URI da memória é removida (agora o app não tem mais uma foto para direcionar)
                                    FileOutputStream fos = getActivity().openFileOutput(nomeFoto, Context.MODE_PRIVATE);
                                    fos.write(null);
                                    fos.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                //Os botões anteriores voltam (de quando não há foto)
                                getActivity().deleteFile(nomeFoto);
                                b_add.setVisibility(View.VISIBLE);
                                b_edit.setVisibility(View.GONE);
                                b_delete.setVisibility(View.GONE);
                                Boolean statusHasPhoto = prefs.edit().putBoolean("hasPhoto", false).commit();
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    myVib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                                } else {
                                    myVib.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
                                }
                                dialog.dismiss();
                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Não",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        });


        //Ação do botão de editar foto:
        b_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    myVib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                } else {
                    myVib.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
                }
                //Verificam-se as permissões:
                if (ContextCompat.checkSelfPermission(
                        getActivity(), Manifest.permission.READ_MEDIA_IMAGES) ==
                        PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                        getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED){
                    //Define-se que é para selecionar somente imagens pelo Intent
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    galleryActivityResultLauncher.launch(intent);
                } else {
                    requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES,Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                }
            }
        });


        //O que o botão de "Sobre" faz:
        b_about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Apenas invoca na tela a mensagem temporária Toast
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    myVib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                } else {
                    myVib.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
                }
                Toast.makeText(getActivity(), "Feito por Thiago Narcizo e por Cauã Candolo", Toast.LENGTH_SHORT).show();
            }
        });

        //O que o botão de adicionar foto faz:
        b_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    myVib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                } else {
                    myVib.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
                }
                //Verificam-se as permissões:
                if (ContextCompat.checkSelfPermission(
                        getActivity(), Manifest.permission.READ_MEDIA_IMAGES) ==
                        PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                        getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED){
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    galleryActivityResultLauncher.launch(intent);
                } else {
                    requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES,Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                }
            }
        });

        return rootView;
    }

    //Método que lida com a solicitação de permissão para acesso à galeria
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        switch (requestCode) {
            case 1: {
                //Se o request for cancelado, o resultado das Arrays é vazio.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED || grantResults.length > 0
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    //Permissão concedida!
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    galleryActivityResultLauncher.launch(intent);
                } else {
                    //Permissão negada!
                    //Precisa avisar ao usuário que a permissão está negada:
                    AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
                    alertDialog.setMessage("Permissão para acessar a galeria negada");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }
                return;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}