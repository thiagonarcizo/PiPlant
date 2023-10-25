package xyz.narcizo.piplant.ui.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Objects;

import pub.devrel.easypermissions.EasyPermissions;
import xyz.narcizo.piplant.MainActivity;
import xyz.narcizo.piplant.R;
import xyz.narcizo.piplant.databinding.FragmentHomeBinding;
import xyz.narcizo.piplant.ui.settings.SettingsFragment;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class HomeFragment extends Fragment {
    public static ImageView iv_foto;
    private FragmentHomeBinding binding;
    private TextView tv_saudacao;
    private TextView tv_mensagem;
    private TextView tv_dia_semana;
    private TextView tv_dia_mes;
    private TextView tv_porcentagem;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String porcentagemRaw;
    private String textNivel;
    private String umidadeRaw;
    private String cor;
    private String api;
    Handler delayhandler = new Handler();
    private Vibrator myVib;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Invoca o SharedPreferences para saber os dados salvos anteriormente
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        porcentagemRaw = prefs.getString("porcentagem", "0%");
        textNivel = prefs.getString("textNivel", "Considere adicionar uma API nas configurações!");
        //Tenta puxar a cor do texto de porcentagem:
        cor = prefs.getString("cor", "verde");
        api = prefs.getString("API", "add");

        //Roda a Thread de puxar da API as informações do sensor
        if (!api.equals("add")) {
            getSensorData();
        }

        //Defino o contexto de onde encontrar os IDs dos arquivos XML
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);
        iv_foto = (ImageView) rootView.findViewById(R.id.foto);
        tv_saudacao = (TextView) rootView.findViewById(R.id.saudacao);
        tv_mensagem = (TextView) rootView.findViewById(R.id.mensagem);
        tv_dia_semana = (TextView) rootView.findViewById(R.id.dia_da_sema);
        tv_dia_mes = (TextView) rootView.findViewById(R.id.data);
        tv_porcentagem = (TextView) rootView.findViewById(R.id.porcentagem);

        myVib = (Vibrator) getContext().getSystemService(Vibrator.class);

        swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh);
        //Defino a cor do refresh como verde (cor padrão do app)
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.verde));

        //Define todos os textos dos TextViews:
        tv_dia_semana.setText(diaSemana());
        tv_dia_mes.setText(diaMes());
        tv_saudacao.setText(saudacao());

        tv_porcentagem.setText(porcentagemRaw);
        tv_mensagem.setText(textNivel);


        //Analisa a cor recebida pelo SharedPreferences e depois seta na interface:
        if (Objects.equals(cor, "vermelho")) {
            tv_porcentagem.setTextColor(getResources().getColor(R.color.vermelho));
        } else if (Objects.equals(cor, "laranja")) {
            tv_porcentagem.setTextColor(getResources().getColor(R.color.laranja));
        } else {
            tv_porcentagem.setTextColor(getResources().getColor(R.color.verde));
        }

        //Tento carregar a foto:
        try {
            System.out.println("Tentando carregar a foto");
            iv_foto.setImageURI(getImage());
        //Caso não haja foto, ele seta como nulo:
        } catch (Exception e) {
            System.out.println("Erro ao carregar a imagem");
            System.out.println(e);
            iv_foto.setImageURI(null);
        }

        //Detecta a ação do refresh:
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                restartActivity();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    myVib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK));
                } else {
                    myVib.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE));
                }
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    //Tenta pegar a URI da imagem salva pelo método do fragment das configurações
    private Uri getImage() {
        FileInputStream fis = null;

        try {
            fis = getActivity().openFileInput("planta");
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String uri = br.readLine();
            br.close();
            fis.close();
            System.out.println(uri);
            return Uri.parse(uri);
        } catch (Exception e) {
            System.out.println("Erro ao carregar no get");
            return null;
        }
    }

    //Pega o nome do dia da semana
    public String diaSemana() {
        Calendar c = Calendar.getInstance();
        int dia = c.get(Calendar.DAY_OF_WEEK);
        String diaSemana = "";

        switch (dia) {
            case Calendar.SUNDAY:
                diaSemana = "Domingo";
                break;
            case Calendar.MONDAY:
                diaSemana = "Segunda-feira";
                break;
            case Calendar.TUESDAY:
                diaSemana = "Terça-feira";
                break;
            case Calendar.WEDNESDAY:
                diaSemana = "Quarta-feira";
                break;
            case Calendar.THURSDAY:
                diaSemana = "Quinta-feira";
                break;
            case Calendar.FRIDAY:
                diaSemana = "Sexta-feira";
                break;
            case Calendar.SATURDAY:
                diaSemana = "Sábado";
                break;
        }

        return diaSemana;
    }


    //Pega o dia do mês e o nome do mês
    public String diaMes() {
        Calendar c = Calendar.getInstance();
        int dia = c.get(Calendar.DAY_OF_MONTH);
        int mes = c.get(Calendar.MONTH);
        String diaMes = "";

        switch (mes) {
            case Calendar.JANUARY:
                diaMes = dia + " de Jan.";
                break;
            case Calendar.FEBRUARY:
                diaMes = dia + " de Fev.";
                break;
            case Calendar.MARCH:
                diaMes = dia + " de Mar.";
                break;
            case Calendar.APRIL:
                diaMes = dia + " de Abr.";
                break;
            case Calendar.MAY:
                diaMes = dia + " de Mai.";
                break;
            case Calendar.JUNE:
                diaMes = dia + " de Jun.";
                break;
            case Calendar.JULY:
                diaMes = dia + " de Jul.";
                break;
            case Calendar.AUGUST:
                diaMes = dia + " de Ago.";
                break;
            case Calendar.SEPTEMBER:
                diaMes = dia + " de Set.";
                break;
            case Calendar.OCTOBER:
                diaMes = dia + " de Out.";
                break;
            case Calendar.NOVEMBER:
                diaMes = dia + " de Nov.";
                break;
            case Calendar.DECEMBER:
                diaMes = dia + " de Dez.";
                break;
        }

        return diaMes;
    }

    //Define se é para exibir "Bom dia", "Boa tarde" ou "Boa noite" com base na hora do dia
    public String saudacao() {
        Calendar c = Calendar.getInstance();
        int hora = c.get(Calendar.HOUR_OF_DAY);
        String saudacao = "";

        if (hora >= 6 && hora < 13) {
            saudacao = "Bom dia";
        } else if (hora >= 13 && hora < 19) {
            saudacao = "Boa tarde";
        } else if (hora >= 19 && hora < 24) {
            saudacao = "Boa noite";
        } else if (hora >= 0 && hora < 6) {
            saudacao = "Boa noite";
        }
        return saudacao;
    }

    //Reinicia a Activity com a ação de refreh
    public void restartActivity() {
        Intent intent = getActivity().getIntent();
        getActivity().finish();
        startActivity(intent);
    }


    //Método que pega a informação da internet via API:
    //(utila-se a classe Jsoup para tratar as informações da web)
    public void getSensorData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final StringBuilder builder = new StringBuilder();

                try {
                    String url = api;
                    Document doc = Jsoup.connect(url).ignoreContentType(true).get();

                    Element body = doc.body();
                    builder.append(body.text());
                } catch (IOException e) {
                    builder.append("Erro: ").append(e.getMessage()).append("\n");
                    umidadeRaw = "0";
                }

                if (getActivity() == null) {
                    return;
                } else {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                umidadeRaw = builder.toString();
                                if (!umidadeRaw.matches("[0-9]+") || umidadeRaw.length() > 4) {
                                    umidadeRaw = "5000";
                                }
                                System.out.println("Valor bruto da umidade: "+umidadeRaw);
                                tv_porcentagem.setText(porcentagemFinal(treatPorcentagem(umidadeRaw)));
                                tv_mensagem.setText(textNivel(treatPorcentagem(umidadeRaw)));
                                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                                Boolean statusPorcentagem = prefs.edit().putString("porcentagem", porcentagemFinal(treatPorcentagem(umidadeRaw))).commit();
                                Boolean statusUmidade = prefs.edit().putInt("porcentagemInt", treatPorcentagem(umidadeRaw)).commit();
                                Boolean statusTextNivel = prefs.edit().putString("textNivel", textNivel(treatPorcentagem(umidadeRaw))).commit();
                                corPorcentagem(treatPorcentagem(umidadeRaw));
                            } catch (Exception e) {
                                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                                porcentagemRaw = prefs.getString("porcentagem", "");
                                textNivel = prefs.getString("textNivel", "");
                                corPorcentagem(treatPorcentagem(umidadeRaw));
                            }
                        }
                    });
                    //Repete a ação a cada 10 segundos!!!
                    delayhandler.postDelayed(this, 10000);
                }
            }
        }).start();
    }


    //Pega o dado bruto do servidor e faz o tratamento em uma escala pré-definida:
    //Umidade em 800: solo muito seco
    //Umidade em 400: solo muito molhado
    public int treatPorcentagem(String p) {
        int porcentagem = Integer.parseInt(p);

        if (porcentagem > 800 && porcentagem != 5000) {
            porcentagem = 800;
        }
        porcentagem -= 400;

        if (porcentagem == 4600) {
            return 5000;
        } else {
            return 100 - (int) Math.round(((porcentagem / 400.0) * 100.0));
        }
    }

    //Adiciona o símbolo de "%" ao valor inteiro da porcentagem:
    public String porcentagemFinal(int d) {
        if (d > 100 && d != 5000) {
            d = 100;
        }

        if (d == 5000) {
            return "Erro!";
        } else {
            return d + "%";
        }
    }

    //Método que define o texto imediatamnte abaixo da porcentagem:
    public String textNivel(int d) {
        if (d <= 15) {
            return "Está na hora de regar a planta!";
        } else if (d <= 30) {
            return "Quase hora de regar!";
        } else if (d <= 65) {
            return "Bons níveis de água ainda!";
        } else if (d == 5000) {
            return "Erro na API. Verifique se a API está correta.";
        } else {
            return "Ótimos níveis de água!";
        }
    }

    //Método que define a cor da exibição da porcentagem (nível de umidade):
    public void corPorcentagem(int d) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (d <= 15 || d == 5000) {
            tv_porcentagem.setTextColor(getResources().getColor(R.color.vermelho));
            Boolean setVermelho = prefs.edit().putString("cor", "vermelho").commit();
        } else if (d <= 30) {
            tv_porcentagem.setTextColor(getResources().getColor(R.color.laranja));
            Boolean setLaranja = prefs.edit().putString("cor", "laranja").commit();
        } else {
            tv_porcentagem.setTextColor(getResources().getColor(R.color.verde));
            Boolean setVerde = prefs.edit().putString("cor", "verde").commit();
        }
    }
}